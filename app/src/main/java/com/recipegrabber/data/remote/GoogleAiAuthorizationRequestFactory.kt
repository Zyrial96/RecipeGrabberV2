package com.recipegrabber.data.remote

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope

object GoogleAiAuthorizationRequestFactory {
    fun generativeLanguageScopeUris(): List<String> {
        return listOf(GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE)
    }

    fun buildGenerativeLanguageRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(generativeLanguageScopeUris().map(::Scope))
            .build()
    }
}
