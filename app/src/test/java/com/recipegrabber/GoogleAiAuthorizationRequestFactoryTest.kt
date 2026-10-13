package com.recipegrabber

import com.recipegrabber.data.remote.GoogleAiAuthorizationRequestFactory
import com.recipegrabber.data.remote.GoogleDriveService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GoogleAiAuthorizationRequestFactoryTest {
    @Test
    fun `files permission is requested only explicitly and excludes drive and cloud platform`() {
        assertEquals(
            listOf(GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE, "https://www.googleapis.com/auth/devstorage.read_only"),
            GoogleAiAuthorizationRequestFactory.generativeLanguageScopeUris(includeFiles = true)
        )
    }

    @Test
    @DisplayName("Should request the Gemini OAuth scope through AuthorizationClient")
    fun `should request gemini oauth scope through authorization client`() {
        assertEquals(
            listOf(GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE),
            GoogleAiAuthorizationRequestFactory.generativeLanguageScopeUris()
        )
    }
}
