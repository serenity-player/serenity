package io.github.serenity_player.serenity.data.api

import io.github.serenity_player.serenity.auth.AuthManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/** Adds a fresh bearer token to every request (refreshing it if needed). */
private class AuthInterceptor(private val auth: AuthManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { auth.accessToken() } ?: throw IOException("Not signed in")
        val request = chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}

/** On a 401, refresh the token and retry the request once. If that fails, the session is over. */
private class TokenAuthenticator(private val auth: AuthManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // already retried once
        val rejected = response.request.header("Authorization")?.removePrefix("Bearer ")
        val fresh = runBlocking { auth.accessTokenAfterUnauthorized(rejected) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }
}

object SpotifyApiFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun create(auth: AuthManager): SpotifyApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(auth))
            .authenticator(TokenAuthenticator(auth))
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.spotify.com/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyApi::class.java)
    }
}
