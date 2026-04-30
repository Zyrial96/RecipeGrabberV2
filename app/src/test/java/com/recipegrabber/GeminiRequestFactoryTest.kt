package com.recipegrabber

import com.recipegrabber.domain.llm.GeminiRequestAuth
import com.recipegrabber.domain.llm.GeminiRequestFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Gemini request factory")
class GeminiRequestFactoryTest {

    @Test
    @DisplayName("Should use API key query parameter for API key auth")
    fun `should use api key query parameter for api key auth`() {
        val request = GeminiRequestFactory.buildGenerateContentRequest(
            model = "gemini-2.5-flash",
            jsonBody = "{}",
            auth = GeminiRequestAuth.ApiKey("test-key")
        )

        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=test-key",
            request.url.toString()
        )
        assertNull(request.header("Authorization"))
    }

    @Test
    @DisplayName("Should use bearer header for OAuth auth")
    fun `should use bearer header for oauth auth`() {
        val request = GeminiRequestFactory.buildGenerateContentRequest(
            model = "gemini-2.5-flash",
            jsonBody = "{}",
            auth = GeminiRequestAuth.OAuth("access-token")
        )

        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            request.url.toString()
        )
        assertEquals("Bearer access-token", request.header("Authorization"))
    }
}
