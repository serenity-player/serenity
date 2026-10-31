package io.github.serenity_player.serenity.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    @Test
    fun challengeMatchesRfc7636Example() {
        // Test vector from RFC 7636, appendix B.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun verifierLengthAndAlphabetAreValid() {
        val verifier = Pkce.verifier()
        assertTrue(verifier.length in 43..128)
        assertTrue(verifier.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun verifiersAndStatesAreRandom() {
        assertNotEquals(Pkce.verifier(), Pkce.verifier())
        assertNotEquals(Pkce.state(), Pkce.state())
    }
}
