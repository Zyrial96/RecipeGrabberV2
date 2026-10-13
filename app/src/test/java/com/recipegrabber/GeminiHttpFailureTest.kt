package com.recipegrabber

import com.recipegrabber.domain.llm.geminiHttpFailure
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeminiHttpFailureTest {
    @Test
    fun `server errors explain temporary service failure not authentication`() {
        for (code in listOf(500, 502, 503, 504)) {
            val message = geminiHttpFailure(code).message.orEmpty()
            assertTrue(message.contains("voruebergehend"))
            assertTrue(message.contains("HTTP $code"))
            assertFalse(message.contains("Schluessel"))
        }
    }

    @Test
    fun `rate limit error asks to check quota`() {
        val message = geminiHttpFailure(429).message.orEmpty()
        assertTrue(message.contains("Kontingent"))
        assertTrue(message.contains("HTTP 429"))
    }

    @Test
    fun `authentication and permissions errors remain distinct`() {
        assertTrue(geminiHttpFailure(401).message.orEmpty().contains("Anmeldung"))
        assertTrue(geminiHttpFailure(403).message.orEmpty().contains("Zugriff"))
    }

    @Test
    fun `unknown error preserves status code without inventing diagnosis`() {
        val message = geminiHttpFailure(418).message.orEmpty()
        assertTrue(message.contains("HTTP 418"))
        assertFalse(message.contains("voruebergehend"))
        assertFalse(message.contains("Anmeldung"))
    }
}
