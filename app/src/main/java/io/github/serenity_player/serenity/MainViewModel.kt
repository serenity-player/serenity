package io.github.serenity_player.serenity

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientIdState(val loaded: Boolean = false, val value: String? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as SerenityApp
    private val auth = container.auth
    private val settings = container.settings

    val authState = auth.state

    val clientId: StateFlow<ClientIdState> = settings.clientId
        .map { ClientIdState(loaded = true, value = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ClientIdState())

    init {
        viewModelScope.launch { auth.init() }
    }

    fun saveClientId(id: String) = viewModelScope.launch { settings.setClientId(id.trim()) }

    fun login(activity: Activity) = viewModelScope.launch { auth.startLogin(activity) }

    fun onRedirect(uri: Uri) = viewModelScope.launch { auth.handleRedirect(uri) }

    fun logout() = viewModelScope.launch { auth.logout() }

    fun changeClientId() = viewModelScope.launch {
        auth.logout()
        settings.clearClientId()
    }
}
