package io.github.serenity_player.serenity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.serenity_player.serenity.ui.SerenityRoot
import io.github.serenity_player.serenity.ui.theme.SerenityTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle the launch intent once, not after rotation/recreation.
        if (savedInstanceState == null) handleRedirect(intent)
        setContent {
            SerenityTheme {
                SerenityRoot(vm = vm, onLogin = { vm.login(this) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        intent?.data?.let(vm::onRedirect)
    }
}
