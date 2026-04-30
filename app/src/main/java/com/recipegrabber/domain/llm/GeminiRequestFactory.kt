package com.recipegrabber.domain.llm

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class GeminiAuthMode {
    API_KEY,
    GOOGLE_OAUTH
}

sealed class GeminiRequestAuth {
    data class ApiKey(val apiKey: String) : GeminiRequestAuth()
    data class OAuth(val accessToken: String) : GeminiRequestAuth()
}

object GeminiRequestFactory {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    fun buildGenerateContentRequest(
        model: String,
        jsonBody: String,
        auth: GeminiRequestAuth
    ): Request {
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val baseUrl = "$BASE_URL/$model:generateContent"
        val builder = Request.Builder()
            .post(requestBody)

        return when (auth) {
            is GeminiRequestAuth.ApiKey -> builder
                .url("$baseUrl?key=${auth.apiKey}")
                .build()
            is GeminiRequestAuth.OAuth -> builder
                .url(baseUrl)
                .header("Authorization", "Bearer ${auth.accessToken}")
                .build()
        }
    }
}
