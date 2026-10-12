package com.recipegrabber.domain.llm

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject

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

    fun buildVideoBody(videoUrl: String, prompt: String): String {
        val url = requireNotNull(videoUrl.toHttpUrlOrNull()) { "Ungueltige Video-URL." }
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) {
            "Bitte eine HTTPS-Video-URL ohne Zugangsdaten verwenden."
        }
        val youtube = url.host == "youtu.be" || url.host == "youtube.com" ||
            url.host.endsWith(".youtube.com")
        val fileData = JsonObject().apply {
            addProperty("fileUri", url.toString())
            if (youtube) {
                require((url.host == "youtu.be" && url.pathSegments.first().isNotBlank()) ||
                    (url.encodedPath == "/watch" && !url.queryParameter("v").isNullOrBlank()) ||
                    (url.pathSegments.first() in listOf("shorts", "live", "embed") &&
                        url.pathSegments.getOrNull(1)?.isNotBlank() == true)) {
                    "Bitte einen YouTube-Videolink statt einer Kanal- oder Startseite verwenden."
                }
            } else {
                val mime = when (url.pathSegments.last().substringAfterLast('.').lowercase()) {
                    "mp4" -> "video/mp4"
                    "webm" -> "video/webm"
                    "mov" -> "video/quicktime"
                    else -> throw IllegalArgumentException(
                        "Dieser Link ist keine direkt lesbare Videodatei. Verwende ein oeffentliches YouTube-Video oder eine HTTPS-MP4-, WebM- oder MOV-Datei."
                    )
                }
                addProperty("mimeType", mime)
            }
        }
        return JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { add("fileData", fileData) })
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("responseMimeType", "application/json")
            })
        }.toString()
    }

    fun buildGenerateContentRequest(
        model: String,
        jsonBody: String,
        auth: GeminiRequestAuth
    ): Request {
        require(model.matches(Regex("gemini-[a-zA-Z0-9.-]+"))) { "Ungueltige Gemini-Modell-ID." }
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val baseUrl = "$BASE_URL/$model:generateContent"
        val builder = Request.Builder()
            .post(requestBody)

        return when (auth) {
            is GeminiRequestAuth.ApiKey -> builder
                .url(baseUrl)
                .header("x-goog-api-key", auth.apiKey)
                .build()
            is GeminiRequestAuth.OAuth -> builder
                .url(baseUrl)
                .header("Authorization", "Bearer ${auth.accessToken}")
                .build()
        }
    }
}
