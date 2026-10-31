package io.github.serenity_player.serenity.player

enum class DeviceKind { Computer, Phone, Speaker, Tv, Cast, Console, Other }

data class Device(
    val id: String,
    val name: String,
    val kind: DeviceKind,
    val isActive: Boolean,
    val isRestricted: Boolean, // Spotify forbids remote control of some devices
    val volumePercent: Int?,
)

data class DevicesUi(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val devices: List<Device> = emptyList(),
    val error: String? = null,
    val transferringId: String? = null,
)

/** One-shot snackbar message; [offerDevices] adds a "Devices" action. */
data class UiMessage(val text: String, val offerDevices: Boolean = false)
