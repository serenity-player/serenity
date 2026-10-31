package io.github.serenity_player.serenity.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.serenity_player.serenity.auth.AuthState

@Composable
fun LoginScreen(state: AuthState, onLogin: () -> Unit, onChangeClientId: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Serenity", style = MaterialTheme.typography.displaySmall)
        Text(
            "Connect your Spotify account to get started.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        if (state is AuthState.Error) {
            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        // Stays enabled while LoggingIn so the user can retry after closing the browser.
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text(if (state is AuthState.LoggingIn) "Waiting for Spotify... tap to retry" else "Connect Spotify")
        }
        TextButton(onClick = onChangeClientId) { Text("Change Client ID") }
    }
}
