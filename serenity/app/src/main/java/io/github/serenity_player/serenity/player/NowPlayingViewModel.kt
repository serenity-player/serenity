package io.github.serenity_player.serenity.player

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.serenity_player.serenity.SerenityApp
import io.github.serenity_player.serenity.data.repository.CommandResult
import io.github.serenity_player.serenity.data.repository.DevicesResult
import io.github.serenity_player.serenity.data.repository.PollResult
import io.github.serenity_player.serenity.data.repository.QueueResult
import io.github.serenity_player.serenity.library.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NowPlayingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SerenityApp).playbackRepository

    private val _ui = MutableStateFlow(NowPlayingUi())
    val ui: StateFlow<NowPlayingUi> = _ui.asStateFlow()

    private val _devices = MutableStateFlow(DevicesUi())
    val devices: StateFlow<DevicesUi> = _devices.asStateFlow()

    /** One-shot messages for the snackbar. */
    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var pollJob: Job? = null
    private var deviceJob: Job? = null
    /** Why live updates are failing right now (offline, rate limited), or null when healthy. Drives a banner. */
    private val _connectionIssue = MutableStateFlow<String?>(null)
    val connectionIssue: StateFlow<String?> = _connectionIssue.asStateFlow()

    /** Set when Spotify rejects a playback command because the account isn't Premium. Drives a banner. */
    private val _premiumRequired = MutableStateFlow(false)
    val premiumRequired: StateFlow<Boolean> = _premiumRequired.asStateFlow()

    /** Bumped on every user command so in-flight poll responses can't overwrite optimistic state. */
    private var epoch = 0

    // ---- Playback polling (foreground only; the UI starts/stops it with the lifecycle) ----

    /** Who currently wants fresh state: the visible UI and/or the notification service. */
    private val pollHolders = mutableSetOf<String>()

    fun startPolling(holder: String = HOLDER_UI) {
        pollHolders += holder
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) delay(poll())
        }
    }

    fun stopPolling(holder: String = HOLDER_UI) {
        pollHolders -= holder
        if (pollHolders.isEmpty()) {
            pollJob?.cancel()
            pollJob = null
        }
    }

    /** Fast while the UI is visible, slower when only the notification service is listening. */
    private fun pollInterval() = if (HOLDER_UI in pollHolders) POLL_MS else BACKGROUND_POLL_MS

    fun reset() {
        pollHolders.clear()
        pollJob?.cancel()
        pollJob = null
        closeDevices()
        closeQueue()
        _connectionIssue.value = null
        _premiumRequired.value = false
        epoch++
        _ui.value = NowPlayingUi()
    }

    /** Fetches once and returns how long to wait before the next poll. */
    private suspend fun poll(): Long {
        val startEpoch = epoch
        val result = repo.fetch()
        if (startEpoch != epoch) return pollInterval() // a command ran meanwhile; it schedules its own refresh
        return when (result) {
            is PollResult.Success -> {
                _connectionIssue.value = null
                _ui.value = NowPlayingUi(
                    loading = false,
                    playback = result.state,
                    syncedAt = SystemClock.elapsedRealtime(),
                )
                pollInterval()
            }
            is PollResult.RateLimited -> {
                notifyFailureOnce("Spotify is rate limiting requests. Backing off.")
                (result.retryAfterSec * 1000L).coerceAtLeast(pollInterval())
            }
            is PollResult.Failure -> {
                notifyFailureOnce(result.message)
                _ui.value = _ui.value.copy(loading = false)
                ERROR_POLL_MS
            }
        }
    }

    private fun notifyFailureOnce(message: String) {
        _connectionIssue.value = message
    }

    // ---- Device picker ----

    fun openDevices() {
        deviceJob?.cancel()
        _devices.value = DevicesUi(visible = true, loading = true)
        // Refresh while the sheet is open: devices appear as soon as Spotify launches somewhere.
        deviceJob = viewModelScope.launch {
            while (isActive) {
                loadDevices()
                delay(DEVICE_REFRESH_MS)
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch { loadDevices() }
    }

    fun closeDevices() {
        deviceJob?.cancel()
        deviceJob = null
        _devices.value = DevicesUi()
    }

    private suspend fun loadDevices() {
        when (val r = repo.devices()) {
            is DevicesResult.Success -> _devices.update { it.copy(loading = false, devices = r.devices, error = null) }
            is DevicesResult.Failure -> _devices.update { it.copy(loading = false, error = r.message) }
        }
    }

    fun transferTo(device: Device) {
        if (device.isRestricted || _devices.value.transferringId != null) return
        val current = _ui.value.playback
        if (device.isActive && current != null) { // already playing there
            closeDevices()
            return
        }
        // Keep the current play/pause state; from a cold start, ask Spotify to start playing.
        val play = current?.isPlaying ?: true
        _devices.update { it.copy(transferringId = device.id, error = null) }
        epoch++
        viewModelScope.launch {
            when (val r = repo.transfer(device.id, play)) {
                is CommandResult.Ok -> {
                    closeDevices()
                    _ui.update { ui -> ui.copy(playback = ui.playback?.copy(deviceName = device.name)) }
                    delay(TRANSFER_SETTLE_MS)
                    poll()
                    if (_ui.value.playback == null) {
                        _messages.send(
                            UiMessage("Switched to ${device.name}, but there's nothing to resume. Start a song in Spotify.")
                        )
                    }
                }
                is CommandResult.Err -> {
                    // Keep the sheet open and show the problem inside it.
                    if (r.premiumRequired) _premiumRequired.value = true
                    _devices.update { it.copy(transferringId = null, error = r.message) }
                }
            }
        }
    }

    private suspend fun report(error: CommandResult.Err) {
        if (error.premiumRequired) _premiumRequired.value = true
        _messages.send(UiMessage(error.message, offerDevices = error.noActiveDevice))
    }

    // ---- Queue ----

    private val _queue = MutableStateFlow(QueueUi())
    val queue: StateFlow<QueueUi> = _queue.asStateFlow()
    private var queueJob: Job? = null

    fun openQueue() {
        queueJob?.cancel()
        _queue.value = QueueUi(visible = true, loading = true)
        // Refresh while open: the queue shifts every time a track ends.
        queueJob = viewModelScope.launch {
            while (isActive) {
                when (val r = repo.queue()) {
                    is QueueResult.Success -> _queue.update {
                        it.copy(loading = false, current = r.current, upcoming = r.upcoming, error = null)
                    }
                    is QueueResult.Failure -> _queue.update { it.copy(loading = false, error = r.message) }
                }
                delay(QUEUE_REFRESH_MS)
            }
        }
    }

    fun closeQueue() {
        queueJob?.cancel()
        queueJob = null
        _queue.value = QueueUi()
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            when (val r = repo.addToQueue(track.uri)) {
                is CommandResult.Ok -> _messages.send(UiMessage("Added to queue: ${track.title}"))
                is CommandResult.Err -> report(r)
            }
        }
    }

    // ---- Play something from the library ----

    /**
     * Starts [request]. With no active device it targets the best available one, which also wakes it up.
     * If [shuffle] is set, shuffle is switched on once playback has started.
     */
    fun play(request: PlayRequest, shuffle: Boolean = false) {
        epoch++
        viewModelScope.launch {
            var targetId: String? = null
            var targetName: String? = null
            if (_ui.value.playback == null) {
                when (val r = repo.devices()) {
                    is DevicesResult.Failure -> {
                        _messages.send(UiMessage(r.message))
                        return@launch
                    }
                    is DevicesResult.Success -> {
                        val usable = r.devices.filter { !it.isRestricted }
                        val device = usable.firstOrNull { it.isActive } ?: usable.firstOrNull()
                        if (device == null) {
                            _messages.send(
                                UiMessage("No device available. Open Spotify on a phone, computer or speaker.", offerDevices = true)
                            )
                            return@launch
                        }
                        targetId = device.id
                        targetName = device.name
                    }
                }
            }
            when (val r = repo.playContent(request, targetId)) {
                is CommandResult.Ok -> {
                    if (shuffle) repo.shuffle(true)
                    targetName?.let { _messages.send(UiMessage("Playing on $it")) }
                    delay(TRANSFER_SETTLE_MS)
                    poll()
                }
                is CommandResult.Err -> report(r)
            }
        }
    }

    // ---- Commands (optimistic update, then confirm with a fresh poll) ----

    fun playPause() {
        val pb = _ui.value.playback ?: return
        if (pb.isPlaying) command({ it.copy(isPlaying = false) }) { repo.pause() }
        else command({ it.copy(isPlaying = true) }) { repo.play() }
    }

    fun next() = command({ it.copy(isPlaying = true, progressMs = 0) }) { repo.next() }

    fun previous() = command({ it.copy(progressMs = 0) }) { repo.previous() }

    fun seek(ms: Long) = command({ it.copy(progressMs = ms) }) { repo.seek(ms) }

    fun setVolume(percent: Int) = command({ it.copy(volumePercent = percent) }) { repo.volume(percent) }

    fun toggleShuffle() {
        val on = !(_ui.value.playback?.shuffle ?: return)
        command({ it.copy(shuffle = on) }) { repo.shuffle(on) }
    }

    fun cycleRepeat() {
        val mode = (_ui.value.playback?.repeat ?: return).next()
        command({ it.copy(repeat = mode) }) { repo.repeat(mode) }
    }

    private fun command(optimistic: (PlaybackState) -> PlaybackState, call: suspend () -> CommandResult) {
        val ui = _ui.value
        val current = ui.playback ?: return
        val now = SystemClock.elapsedRealtime()
        // Freeze the live position first so pausing/toggling doesn't jump the progress bar.
        val base = current.copy(progressMs = current.positionAt(ui.syncedAt, now))
        epoch++
        _ui.value = ui.copy(playback = optimistic(base), syncedAt = now)

        viewModelScope.launch {
            val result = call()
            when (result) {
                is CommandResult.Err -> report(result)
                is CommandResult.Ok -> _premiumRequired.value = false // a command worked, so Premium is fine
            }
            delay(if (result is CommandResult.Ok) SETTLE_MS else 0L)
            poll() // confirms the change, or reverts the optimistic update on failure
        }
    }

    private companion object {
        const val POLL_MS = 3_000L
        const val ERROR_POLL_MS = 6_000L
        const val BACKGROUND_POLL_MS = 6_000L
        const val HOLDER_UI = "ui"
        const val SETTLE_MS = 600L // Spotify's state lags slightly behind commands
        const val TRANSFER_SETTLE_MS = 1_000L
        const val DEVICE_REFRESH_MS = 5_000L
        const val QUEUE_REFRESH_MS = 5_000L
    }
}
