package com.recipegrabber.data.remote

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope

object GoogleAiAuthorizationRequestFactory {
    fun generativeLanguageScopeUris(includeFiles: Boolean = false): List<String> {
        return buildList {
            add(GoogleDriveService.GENERATIVE_LANGUAGE_SCOPE)
            if (includeFiles) add(GoogleDriveService.GEMINI_FILES_SCOPE)
        }
    }

    fun buildGenerativeLanguageRequest(includeFiles: Boolean = false): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(generativeLanguageScopeUris(includeFiles).map(::Scope))
            .build()
    }
}
