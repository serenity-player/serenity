package io.github.serenity_player.serenity.auth

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.github.serenity_player.serenity.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data object LoggingIn : AuthState
    data object LoggedIn : AuthState
    data class Error(val message: String) : AuthState
}

class AuthException(val code: Int, message: String) : IOException(message)

class AuthManager(
    private val tokens: TokenStore,
    private val settings: SettingsStore,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val refreshLock = Mutex()

    /** Call once at startup. */
    suspend fun init() {
        if (_state.value == AuthState.Loading) {
            _state.value = if (tokens.load() != null) AuthState.LoggedIn else AuthState.LoggedOut
        }
    }

    suspend fun startLogin(activity: Activity) {
        val clientId = settings.clientId.first() ?: return fail("Enter a Client ID first.")
        val verifier = Pkce.verifier()
        val state = Pkce.state()
        tokens.savePending(PendingLogin(verifier, state))
        _state.value = AuthState.LoggingIn

        val uri = Uri.Builder()
            .scheme("https").authority("accounts.spotify.com").path("authorize")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", Pkce.challenge(verifier))
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", SCOPES.joinToString(" "))
            .build()
        CustomTabsIntent.Builder().build().launchUrl(activity, uri)
    }

    suspend fun handleRedirect(uri: Uri) {
        if (uri.scheme != "serenity" || uri.host != "callback") return
        val pending = tokens.loadPending() ?: return fail("Login session expired. Please try again.")
        tokens.clearPending()

        uri.getQueryParameter("error")?.let { return fail("Spotify returned: $it") }
        if (uri.getQueryParameter("state") != pending.state) return fail("Login state mismatch. Please try again.")
        val code = uri.getQueryParameter("code") ?: return fail("No authorization code received.")
        val clientId = settings.clientId.first() ?: return fail("Client ID missing.")

        try {
            val t = requestToken(
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "redirect_uri" to REDIRECT_URI,
                    "client_id" to clientId,
                    "code_verifier" to pending.verifier,
                )
            )
            tokens.save(t)
            _state.value = AuthState.LoggedIn
        } catch (e: IOException) {
            fail(e.message ?: "Network error during login.")
        }
    }

    /** A valid access token, refreshing if it expires within 60s. Null if logged out or offline. */
    suspend fun accessToken(): String? = refreshLock.withLock {
        val current = tokens.load() ?: return null
        if (current.expiresAtMs - System.currentTimeMillis() > 60_000) return current.access
        refresh(current)
    }

    /** After a 401: refresh unless another caller already replaced the rejected token. */
    suspend fun accessTokenAfterUnauthorized(rejected: String?): String? = refreshLock.withLock {
        val current = tokens.load() ?: return null
        if (current.access != rejected) return current.access
        refresh(current)
    }

    private suspend fun refresh(current: Tokens): String? {
        val clientId = settings.clientId.first() ?: return null
        return try {
            val fresh = requestToken(
                mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to current.refresh,
                    "client_id" to clientId,
                ),
                oldRefresh = current.refresh,
            )
            tokens.save(fresh)
            fresh.access
        } catch (e: AuthException) {
            // Revoked or invalid refresh token: say so, rather than silently dropping to the login screen.
            if (e.code == 400 || e.code == 401) {
                tokens.clear()
                fail("Your Spotify session expired. Please connect again.")
            }
            null
        } catch (e: IOException) {
            null // Offline: keep the session, try again later.
        }
    }

    suspend fun logout() {
        tokens.clear()
        _state.value = AuthState.LoggedOut
    }

    private fun fail(message: String) {
        _state.value = AuthState.Error(message)
    }

    private suspend fun requestToken(form: Map<String, String>, oldRefresh: String? = null): Tokens =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
            val request = Request.Builder().url(TOKEN_URL).post(body).build()
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val desc = runCatching { JSONObject(text).optString("error_description") }.getOrDefault("")
                    throw AuthException(resp.code, desc.ifBlank { "Token request failed (HTTP ${resp.code})" })
                }
                val json = JSONObject(text)
                val newRefresh = json.optString("refresh_token", "").takeIf { it.isNotBlank() }
                    ?: oldRefresh
                    ?: throw AuthException(resp.code, "No refresh token returned.")
                Tokens(
                    access = json.getString("access_token"),
                    refresh = newRefresh,
                    expiresAtMs = System.currentTimeMillis() + json.getLong("expires_in") * 1000,
                )
            }
        }

    companion object {
        const val REDIRECT_URI = "serenity://callback"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        val SCOPES = listOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "user-read-private",
            "user-library-read",
            "playlist-read-private",
            "user-follow-read",
        )
    }
}
