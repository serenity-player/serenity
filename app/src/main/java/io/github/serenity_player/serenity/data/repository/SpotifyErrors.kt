package io.github.serenity_player.serenity.data.repository

import org.json.JSONObject
import retrofit2.Response

internal data class SpotifyError(
    val message: String,
    val noActiveDevice: Boolean = false,
    val premiumRequired: Boolean = false,
    val notRegistered: Boolean = false,
)

/** Turns a failed Web API response into something a person can act on. */
internal fun spotifyError(r: Response<*>): SpotifyError {
    val error = runCatching { JSONObject(r.errorBody()?.string().orEmpty()).getJSONObject("error") }.getOrNull()
    val reason = error?.optString("reason").orEmpty()
    val detail = error?.optString("message").orEmpty()
    val retryAfter = r.headers()["Retry-After"]?.toIntOrNull()

    return when {
        reason == "NO_ACTIVE_DEVICE" ->
            SpotifyError("No active device. Pick one to start playing.", noActiveDevice = true)

        reason == "PREMIUM_REQUIRED" ->
            SpotifyError("Spotify Premium is required for playback control.", premiumRequired = true)

        r.code() == 429 -> SpotifyError(
            if (retryAfter != null) "Spotify is rate limiting requests. Try again in $retryAfter seconds."
            else "Spotify is rate limiting requests. Try again shortly."
        )

        r.code() == 401 -> SpotifyError("Session expired. Please log in again.")

        // Dev-mode apps only work for accounts added to the app in the Spotify dashboard.
        r.code() == 403 && detail.contains("not registered", ignoreCase = true) -> SpotifyError(
            "This Spotify account isn't added to your developer app. Add it under User Management in the Spotify dashboard.",
            notRegistered = true,
        )

        else -> SpotifyError("Spotify couldn't do that (error ${r.code()}).")
    }
}
