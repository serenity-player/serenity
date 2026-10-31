package io.github.serenity_player.serenity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.serenity_player.serenity.MainViewModel
import io.github.serenity_player.serenity.auth.AuthState
import io.github.serenity_player.serenity.ui.login.LoginScreen
import io.github.serenity_player.serenity.ui.setup.SetupScreen

@Composable
fun SerenityRoot(vm: MainViewModel, onLogin: () -> Unit) {
    val clientId by vm.clientId.collectAsStateWithLifecycle()
    val auth by vm.authState.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        when {
            !clientId.loaded || auth is AuthState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            clientId.value == null -> SetupScreen(onSave = vm::saveClientId)

            auth is AuthState.LoggedIn -> MainScaffold(onLogout = vm::logout)

            else -> LoginScreen(
                state = auth,
                onLogin = onLogin,
                onChangeClientId = vm::changeClientId,
            )
        }
    }
}
