package io.github.serenity_player.serenity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.R

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        @Suppress("DEPRECATION")
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Serenity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Version $version", style = MaterialTheme.typography.labelLarge)
                Text("A Material You remote for Spotify. Playback happens in the Spotify app on your devices; Serenity controls it.")
                Text(
                    "Music data, artwork and playback are provided by Spotify. Serenity is an independent open-source " +
                        "project and is not affiliated with or endorsed by Spotify.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(context.getString(R.string.repo_url)) }) { Text("Source code") }
        },
    )
}
