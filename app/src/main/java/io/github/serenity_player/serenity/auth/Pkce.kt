package io.github.serenity_player.serenity.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object Pkce {
    // java.util.Base64 (not android.util) so this can be unit tested on the JVM.
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    private fun randomString(bytes: Int): String {
        val b = ByteArray(bytes)
        SecureRandom().nextBytes(b)
        return encoder.encodeToString(b)
    }

    fun verifier(): String = randomString(64) // 86 chars, within the 43-128 limit

    fun state(): String = randomString(16)

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return encoder.encodeToString(digest)
    }
}
