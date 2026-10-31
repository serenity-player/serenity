package io.github.serenity_player.serenity.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDs by preferencesDataStore("serenity_settings")

/** The Client ID is read from exactly one place: here. */
class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("client_id")

    val clientId: Flow<String?> = context.settingsDs.data.map { it[key] }

    suspend fun setClientId(id: String) {
        context.settingsDs.edit { it[key] = id }
    }

    suspend fun clearClientId() {
        context.settingsDs.edit { it.remove(key) }
    }

    companion object {
        private val CLIENT_ID_REGEX = Regex("^[0-9a-fA-F]{32}$")
        fun isValidClientId(id: String) = CLIENT_ID_REGEX.matches(id.trim())
    }
}
