package com.recipegrabber.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.recipegrabber.domain.backup.RecipeBackupCodec
import com.recipegrabber.domain.llm.consumeGeminiResponse
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class DriveArchive(val id: String, val createdTime: String, val recipeCount: Int)

@Singleton
class DriveBackupTransport internal constructor(private val calls: Call.Factory) {
    @Inject constructor() : this(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(60, TimeUnit.SECONDS).build())

    suspend fun upload(token: String, bytes: ByteArray, count: Int): String {
        require(bytes.size in 1..RecipeBackupCodec.MAX_BYTES && count in 0..RecipeBackupCodec.MAX_RECIPES)
        val metadata = JsonObject().apply {
            addProperty("name", "RecipeGrabber-${System.currentTimeMillis()}.json")
            addProperty("mimeType", "application/json")
            add("parents", com.google.gson.JsonArray().apply { add("appDataFolder") })
            add("appProperties", JsonObject().apply {
                addProperty("recipegrabberFormat", "1")
                addProperty("recipeCount", count.toString())
            })
        }
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addPart(bytes.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        return calls.newCall(request("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id", token)
            .post(body).build()).consumeGeminiResponse { response ->
            val id = JsonParser.parseString(read(response, 64 * 1024).toString(Charsets.UTF_8)).asJsonObject.get("id").asString
            require(validId(id)) { "Invalid Drive file identifier" }
            id
        }
    }

    suspend fun list(token: String): List<DriveArchive> {
        val archives = mutableListOf<DriveArchive>()
        var page: String? = null
        val seen = mutableSetOf<String>()
        repeat(10) {
            val url = okhttp3.HttpUrl.Builder().scheme("https").host("www.googleapis.com").addPathSegments("drive/v3/files")
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "trashed = false and mimeType = 'application/json' and appProperties has { key='recipegrabberFormat' and value='1' }")
                .addQueryParameter("fields", "nextPageToken,files(id,createdTime,appProperties)")
                .addQueryParameter("orderBy", "createdTime desc").addQueryParameter("pageSize", "100")
                .apply { page?.let { addQueryParameter("pageToken", it) } }.build()
            val root = calls.newCall(request(url.toString(), token).build()).consumeGeminiResponse { response ->
                JsonParser.parseString(read(response, 256 * 1024).toString(Charsets.UTF_8)).asJsonObject
            }
            root.getAsJsonArray("files").forEach { entry ->
                val file = entry.asJsonObject
                val id = file.get("id").asString
                val properties = file.getAsJsonObject("appProperties")
                val count = properties?.get("recipeCount")?.asString?.toIntOrNull()
                val time = file.get("createdTime").asString
                if (validId(id) && properties?.get("recipegrabberFormat")?.asString == "1" &&
                    count != null && count in 0..RecipeBackupCodec.MAX_RECIPES && time.length <= 64) {
                    java.time.Instant.parse(time)
                    if (archives.none { it.id == id }) archives += DriveArchive(id, time, count)
                }
            }
            page = root.get("nextPageToken")?.takeUnless { it.isJsonNull }?.asString
            if (page.isNullOrBlank()) return archives
            require(page!!.length <= 4096 && seen.add(page!!)) { "Invalid Drive pagination" }
        }
        throw IOException("Too many Drive backups; archive listing is incomplete")
    }

    suspend fun download(token: String, id: String): ByteArray {
        require(validId(id)) { "Invalid Drive file identifier" }
        return calls.newCall(request("https://www.googleapis.com/drive/v3/files/$id?alt=media", token).build())
            .consumeGeminiResponse { read(it, RecipeBackupCodec.MAX_BYTES) }
    }

    private fun request(url: String, token: String): Request.Builder {
        require(token.isNotBlank() && token.length <= 16384 && token.none { it.isISOControl() })
        return Request.Builder().url(url).header("Authorization", "Bearer $token")
    }

    private fun read(response: Response, limit: Int): ByteArray {
        if (!response.isSuccessful) throw IOException("Drive request failed (HTTP ${response.code})")
        val body = response.body ?: throw IOException("Drive response unavailable")
        if (body.contentLength() > limit) throw IOException("Drive response exceeds size limit")
        val output = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > limit) throw IOException("Drive response exceeds size limit")
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    private fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_-]{1,200}"))
}
