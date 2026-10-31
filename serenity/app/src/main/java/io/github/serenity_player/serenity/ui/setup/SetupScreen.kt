package io.github.serenity_player.serenity.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.auth.AuthManager
import io.github.serenity_player.serenity.data.settings.SettingsStore

@Composable
fun SetupScreen(onSave: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val valid = SettingsStore.isValidClientId(input)
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome to Serenity", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Serenity needs your own free Spotify developer app. It takes about two minutes. " +
                "Spotify Premium is required for playback control.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Create an app in the dashboard", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { uriHandler.openUri("https://developer.spotify.com/dashboard") }) {
                    Text("Open Spotify Developer Dashboard")
                }
                Text("2. Add this Redirect URI to the app", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        AuthManager.REDIRECT_URI,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { clipboard.setText(AnnotatedString(AuthManager.REDIRECT_URI)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy redirect URI")
                    }
                }
                Text("3. Select \"Web API\" when asked which APIs you plan to use", style = MaterialTheme.typography.titleSmall)
                Text("4. Paste the Client ID below", style = MaterialTheme.typography.titleSmall)
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.trim() },
            label = { Text("Client ID") },
            singleLine = true,
            isError = input.isNotEmpty() && !valid,
            supportingText = {
                if (input.isNotEmpty() && !valid) Text("Should be 32 hexadecimal characters")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = { onSave(input) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
            Text("Save and continue")
        }

        Text(
            "Not affiliated with or endorsed by Spotify.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
