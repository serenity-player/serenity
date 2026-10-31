package io.github.serenity_player.serenity.data.repository

import io.github.serenity_player.serenity.data.api.DeviceDto
import io.github.serenity_player.serenity.data.api.OffsetDto
import io.github.serenity_player.serenity.data.api.PlayBody
import io.github.serenity_player.serenity.data.api.PlaybackStateDto
import io.github.serenity_player.serenity.data.api.SpotifyApi
import io.github.serenity_player.serenity.data.api.TransferBody
import io.github.serenity_player.serenity.player.Device
import io.github.serenity_player.serenity.player.DeviceKind
import io.github.serenity_player.serenity.player.PlayRequest
import io.github.serenity_player.serenity.player.PlaybackState
import io.github.serenity_player.serenity.player.RepeatMode
import org.json.JSONObject
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

sealed interface PollResult {
    data class Success(val state: PlaybackState?) : PollResult
    data class RateLimited(val retryAfterSec: Int) : PollResult
    data class Failure(val message: String) : PollResult
}

sealed interface CommandResult {
    data object Ok : CommandResult
    data class Err(
        val message: String,
        val noActiveDevice: Boolean = false,
        val premiumRequired: Boolean = false,
    ) : CommandResult
}

sealed interface DevicesResult {
    data class Success(val devices: List<Device>) : DevicesResult
    data class Failure(val message: String) : DevicesResult
}

class PlaybackRepository(private val api: SpotifyApi) {

    suspend fun fetch(): PollResult = try {
        val r = api.getPlayback()
        when {
            r.code() == 204 -> PollResult.Success(null)
            r.isSuccessful -> PollResult.Success(r.body()?.toDomain())
            r.code() == 429 -> PollResult.RateLimited(r.headers()["Retry-After"]?.toIntOrNull() ?: 5)
            else -> PollResult.Failure(errorFor(r).message)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PollResult.Failure("Can't reach Spotify. Retrying...")
    }

    suspend fun devices(): DevicesResult = try {
        val r = api.getDevices()
        if (r.isSuccessful) {
            DevicesResult.Success(r.body()?.devices.orEmpty().mapNotNull { it.toDomain() })
        } else {
            DevicesResult.Failure(errorFor(r).message)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DevicesResult.Failure("Can't reach Spotify.")
    }

    suspend fun queue(): QueueResult = try {
        val r = api.getQueue()
        val body = r.body()
        if (r.isSuccessful && body != null) {
            QueueResult.Success(
                current = body.currentlyPlaying?.toTrack(null),
                upcoming = body.queue.mapNotNull { it.toTrack(null) },
            )
        } else {
            QueueResult.Failure(errorFor(r).message)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        QueueResult.Failure("Can't reach Spotify.")
    }

    /** Needs an active device; the caller turns NO_ACTIVE_DEVICE into a "pick a device" prompt. */
    suspend fun addToQueue(uri: String) = command { api.addToQueue(uri) }

    suspend fun transfer(deviceId: String, play: Boolean) =
        command { api.transfer(TransferBody(listOf(deviceId), play)) }

    suspend fun play() = command { api.play() }

    suspend fun playContent(request: PlayRequest, deviceId: String?) = command {
        val body = when (request) {
            is PlayRequest.Context -> PlayBody(
                contextUri = request.uri,
                offset = request.offsetUri?.let { OffsetDto(uri = it) },
            )
            is PlayRequest.Tracks -> PlayBody(uris = request.uris.take(100)) // API max is 100 per request
        }
        api.playContent(deviceId, body)
    }
    suspend fun pause() = command { api.pause() }
    suspend fun next() = command { api.next() }
    suspend fun previous() = command { api.previous() }
    suspend fun seek(ms: Long) = command { api.seek(ms) }
    suspend fun shuffle(on: Boolean) = command { api.shuffle(on) }
    suspend fun repeat(mode: RepeatMode) = command { api.repeat(mode.api) }
    suspend fun volume(percent: Int) = command { api.volume(percent) }

    private suspend fun command(call: suspend () -> Response<Unit>): CommandResult = try {
        val r = call()
        if (r.isSuccessful) CommandResult.Ok else errorFor(r)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CommandResult.Err("Can't reach Spotify.")
    }

    private fun errorFor(r: Response<*>): CommandResult.Err {
        val e = spotifyError(r)
        return CommandResult.Err(e.message, noActiveDevice = e.noActiveDevice, premiumRequired = e.premiumRequired)
    }
}

private fun DeviceDto.toDomain(): Device? {
    val deviceId = id ?: return null // devices without an id can't be targeted
    return Device(
        id = deviceId,
        name = name,
        kind = when (type.lowercase()) {
            "computer" -> DeviceKind.Computer
            "smartphone" -> DeviceKind.Phone
            "speaker", "avr", "stb", "audiodongle" -> DeviceKind.Speaker
            "tv" -> DeviceKind.Tv
            "castaudio", "castvideo" -> DeviceKind.Cast
            "gameconsole" -> DeviceKind.Console
            else -> DeviceKind.Other
        },
        isActive = isActive,
        isRestricted = isRestricted,
        volumePercent = volumePercent,
    )
}

private fun PlaybackStateDto.toDomain(): PlaybackState? {
    val d = device ?: return null
    val i = item
    val images = i?.album?.images?.takeIf { it.isNotEmpty() } ?: i?.images.orEmpty()
    return PlaybackState(
        title = i?.name?.takeIf { it.isNotBlank() }
            ?: if (type == "ad") "Advertisement" else "Nothing playing",
        subtitle = i?.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }
            ?: i?.show?.name.orEmpty(),
        album = i?.album?.name,
        artUrl = images.firstOrNull()?.url, // Spotify lists the largest image first
        isPlaying = isPlaying,
        progressMs = progressMs ?: 0L,
        durationMs = i?.durationMs ?: 0L,
        shuffle = shuffleState,
        repeat = RepeatMode.from(repeatState),
        deviceName = d.name,
        volumePercent = d.volumePercent,
        supportsVolume = d.supportsVolume && d.volumePercent != null,
    )
}
