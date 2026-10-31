package io.github.serenity_player.serenity.auth

import io.github.serenity_player.serenity.data.settings.SettingsStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientIdTest {
    @Test
    fun acceptsThirtyTwoHexCharacters() {
        assertTrue(SettingsStore.isValidClientId("0123456789abcdef0123456789ABCDEF"))
    }

    @Test
    fun ignoresSurroundingWhitespace() {
        assertTrue(SettingsStore.isValidClientId("  0123456789abcdef0123456789abcdef \n"))
    }

    @Test
    fun rejectsWrongLengthOrNonHex() {
        assertFalse(SettingsStore.isValidClientId(""))
        assertFalse(SettingsStore.isValidClientId("0123456789abcdef"))
        assertFalse(SettingsStore.isValidClientId("0123456789abcdef0123456789abcdeg"))
        assertFalse(SettingsStore.isValidClientId("0123456789abcdef0123456789abcdef0"))
    }
}
