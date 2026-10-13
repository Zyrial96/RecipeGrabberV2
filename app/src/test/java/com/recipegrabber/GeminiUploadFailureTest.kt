package com.recipegrabber

import com.recipegrabber.domain.llm.geminiUploadHttpFailure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeminiUploadFailureTest {
    @Test fun `scope error distinguishes file access from working Youtube authorization`() {
        val message = geminiUploadHttpFailure(403, """{"error":{"details":[{"reason":"ACCESS_TOKEN_SCOPE_INSUFFICIENT"}]}}""").message.orEmpty()
        assertTrue(message.contains("Zusatzberechtigung"))
        assertTrue(message.contains("ACCESS_TOKEN_SCOPE_INSUFFICIENT"))
        assertTrue(message.contains("HTTP 403"))
    }

    @Test fun `unknown forbidden response does not invent a scope diagnosis`() {
        val message = geminiUploadHttpFailure(403, "{}").message.orEmpty()
        assertFalse(message.contains("Zusatzberechtigung"))
        assertTrue(message.contains("HTTP 403"))
    }

    @Test fun `malformed or raw server values are not copied into logs or errors`() {
        for (response in listOf("private-token-value", """{"error":{"message":"private-token-value","details":[{"reason":"https://private.example/?token=private-token-value"}]}}""")) {
            val message = geminiUploadHttpFailure(403, response).message.orEmpty()
            assertFalse(message.contains("private-token-value"))
            assertFalse(message.contains("private.example"))
        }
    }
}
