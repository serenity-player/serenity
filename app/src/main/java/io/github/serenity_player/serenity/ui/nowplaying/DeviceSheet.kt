package io.github.serenity_player.serenity.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.player.Device
import io.github.serenity_player.serenity.player.DeviceKind
import io.github.serenity_player.serenity.player.DevicesUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSheet(
    state: DevicesUi,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (Device) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Connect to a device", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh devices") }
            }

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            when {
                state.loading && state.devices.isEmpty() ->
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }

                state.devices.isEmpty() ->
                    Text(
                        "No devices found. Open Spotify on a phone, computer or speaker and it will appear here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    )

                else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.devices.forEach { device ->
                        DeviceRow(device, transferring = state.transferringId == device.id, enabled = state.transferringId == null, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, transferring: Boolean, enabled: Boolean, onSelect: (Device) -> Unit) {
    val usable = !device.isRestricted
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = {
            Text(
                when {
                    device.isRestricted -> "Can't be controlled remotely"
                    device.isActive -> "Playing here"
                    else -> device.kind.label()
                }
            )
        },
        leadingContent = { Icon(device.kind.icon(), contentDescription = null) },
        trailingContent = {
            when {
                transferring -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                device.isActive -> Icon(Icons.Filled.GraphicEq, contentDescription = "Active device", tint = MaterialTheme.colorScheme.primary)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (device.isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        ),
        modifier = Modifier.clickable(enabled = usable && enabled) { onSelect(device) },
    )
}

private fun DeviceKind.icon(): ImageVector = when (this) {
    DeviceKind.Computer -> Icons.Filled.Computer
    DeviceKind.Phone -> Icons.Filled.Smartphone
    DeviceKind.Speaker -> Icons.Filled.Speaker
    DeviceKind.Tv -> Icons.Filled.Tv
    DeviceKind.Cast -> Icons.Filled.Cast
    DeviceKind.Console -> Icons.Filled.SportsEsports
    DeviceKind.Other -> Icons.Filled.Devices
}

private fun DeviceKind.label(): String = when (this) {
    DeviceKind.Computer -> "Computer"
    DeviceKind.Phone -> "Phone"
    DeviceKind.Speaker -> "Speaker"
    DeviceKind.Tv -> "TV"
    DeviceKind.Cast -> "Cast device"
    DeviceKind.Console -> "Game console"
    DeviceKind.Other -> "Device"
}
