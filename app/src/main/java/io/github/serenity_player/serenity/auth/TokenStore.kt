package io.github.serenity_player.serenity.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.tokenDs by preferencesDataStore("serenity_tokens")

data class Tokens(val access: String, val refresh: String, val expiresAtMs: Long)

/** Pending PKCE login: survives process death while the user is in the browser. */
data class PendingLogin(val verifier: String, val state: String)

class TokenStore(private val context: Context) {
    private val access = stringPreferencesKey("access")
    private val refresh = stringPreferencesKey("refresh")
    private val expires = longPreferencesKey("expires_at")
    private val pending = stringPreferencesKey("pending")

    suspend fun load(): Tokens? {
        val p = context.tokenDs.data.first()
        val a = p[access]?.let(Crypto::decrypt)
        val r = p[refresh]?.let(Crypto::decrypt)
        val e = p[expires]
        return if (a != null && r != null && e != null) Tokens(a, r, e) else null
    }

    suspend fun save(t: Tokens) {
        context.tokenDs.edit {
            it[access] = Crypto.encrypt(t.access)
            it[refresh] = Crypto.encrypt(t.refresh)
            it[expires] = t.expiresAtMs
        }
    }

    suspend fun clear() {
        context.tokenDs.edit {
            it.remove(access); it.remove(refresh); it.remove(expires); it.remove(pending)
        }
    }

    suspend fun savePending(p: PendingLogin) {
        context.tokenDs.edit { it[pending] = Crypto.encrypt("${p.verifier}|${p.state}") }
    }

    suspend fun loadPending(): PendingLogin? {
        val raw = context.tokenDs.data.first()[pending]?.let(Crypto::decrypt) ?: return null
        val parts = raw.split("|")
        return if (parts.size == 2) PendingLogin(parts[0], parts[1]) else null
    }

    suspend fun clearPending() {
        context.tokenDs.edit { it.remove(pending) }
    }
}
