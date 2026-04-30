package com.recipegrabber

import com.recipegrabber.data.remote.GoogleAiAuthorizationRequestFactory
import com.recipegrabber.data.remote.GoogleDriveService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GoogleAiAuthorizationRequestFactoryTest {

    @Test
    @DisplayName("Should request the Gemini OAuth scope through AuthorizationClient")
    fun `should request gemini oauth scope through authorization client`() {
        assertEquals(
            listOf(GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE),
            GoogleAiAuthorizationRequestFactory.generativeLanguageScopeUris()
        )
    }
}
