package com.recipegrabber.domain.llm

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

internal class GeminiFileUploader(
    private val downloads: Call.Factory,
    private val api: Call.Factory,
    private val cacheDir: File,
    private val apiRoot: HttpUrl = "https://generativelanguage.googleapis.com/".toHttpUrl(),
    private val maxBytes: Long = 100L * 1024 * 1024,
    private val preparationTimeoutMillis: Long = 180_000,
    private val pollMillis: Long = 2_000,
    private val cleanupApi: Call.Factory = api
) {
    suspend fun <T> withVideoBody(source: String, prompt: String, auth: GeminiRequestAuth, consume: suspend (String) -> T): T {
        val body = JsonParser.parseString(GeminiRequestFactory.buildVideoBody(source, prompt)).asJsonObject
        val media = body.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")[0].asJsonObject.getAsJsonObject("fileData")
        val mime = media.get("mimeType")?.asString ?: return consume(body.toString())
        val temporary = File.createTempFile("recipe-video-", ".media", cacheDir)
        var remoteName: String? = null
        try {
            withTimeout(preparationTimeoutMillis) {
                download(source.toHttpUrl(), temporary)
                val metadata = """{"file":{"display_name":"RecipeGrabber video"}}"""
                val start = authorized(Request.Builder().url(apiRoot.resolve("upload/v1beta/files")!!), auth)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", temporary.length().toString())
                    .header("X-Goog-Upload-Header-Content-Type", mime)
                    .post(metadata.toRequestBody("application/json".toMediaType())).build()
                val session = api.newCall(start).consumeGeminiResponse { response ->
                    if (!response.isSuccessful) throw geminiUploadHttpFailure(response.code, response.body?.string())
                    requireNotNull(response.header("X-Goog-Upload-URL")) { "Google hat keine Upload-Adresse geliefert." }.toHttpUrl()
                }
                require(sameOrigin(session, apiRoot)) { "Unerwartete Video-Upload-Adresse." }
                val upload = authorized(Request.Builder().url(session), auth)
                    .header("X-Goog-Upload-Offset", "0")
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .post(temporary.asRequestBody(mime.toMediaType())).build()
                var file = readJson(upload).getAsJsonObject("file")
                    ?: throw IllegalArgumentException("Google hat keine hochgeladene Datei geliefert.")
                remoteName = checkedName(file)
                while (file.get("state")?.asString == "PROCESSING") {
                    delay(pollMillis)
                    file = readJson(authorized(Request.Builder().url(apiRoot.resolve("v1beta/$remoteName")!!), auth).get().build())
                    require(checkedName(file) == remoteName) { "Unerwartete Google-Dateiantwort." }
                }
                require(file.get("state")?.asString == "ACTIVE") { "Google konnte die Videodatei nicht verarbeiten." }
                val uri = requireNotNull(file.get("uri")?.asString).toHttpUrl()
                require(sameOrigin(uri, apiRoot) && uri.encodedPath == "/v1beta/$remoteName") { "Unerwartete Google-Dateiadresse." }
                media.addProperty("fileUri", uri.toString())
            }
            return consume(body.toString())
        } finally {
            temporary.delete()
            remoteName?.let { name ->
                // Delete only the file created by this request; cancellation must not skip cleanup.
                withContext(NonCancellable) {
                    try {
                        cleanupApi.newCall(authorized(Request.Builder().url(apiRoot.resolve("v1beta/$name")!!), auth).delete().build())
                            .consumeGeminiResponse { Unit }
                    } catch (_: Exception) {
                        // Google expires uploaded files; cleanup failure must not replace the recipe result.
                    }
                }
            }
        }
    }

    private suspend fun download(initial: HttpUrl, target: File) {
        var url = initial
        repeat(6) {
            require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "Unsichere Video-Weiterleitung." }
            val redirect = downloads.newCall(Request.Builder().url(url)
                .header("User-Agent", "RecipeGrabberV2 (https://github.com/Zyrial96/RecipeGrabberV2)")
                .build()).consumeGeminiResponse { response ->
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    requireNotNull(response.header("Location")?.let(url::resolve)) { "Ungueltige Video-Weiterleitung." }
                } else {
                    require(response.isSuccessful) { "Videodatei konnte nicht geladen werden (HTTP ${response.code})." }
                    val body = requireNotNull(response.body) { "Leere Videodatei." }
                    require(body.contentLength() <= maxBytes) { "Videodatei ist zu gross (maximal 100 MB)." }
                    require(response.header("Content-Type")?.substringBefore(';') !in listOf("text/html", "application/json")) { "Der Link liefert keine Videodatei." }
                    var total = 0L
                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                total += count
                                require(total <= maxBytes) { "Videodatei ist zu gross (maximal 100 MB)." }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    require(total > 0) { "Leere Videodatei." }
                    null
                }
            }
            if (redirect == null) return
            url = redirect
        }
        throw IllegalArgumentException("Zu viele Video-Weiterleitungen.")
    }

    private suspend fun readJson(request: Request): JsonObject = api.newCall(request).consumeGeminiResponse { response ->
        require(response.isSuccessful) { "Google-Dateiverarbeitung fehlgeschlagen (HTTP ${response.code})." }
        JsonParser.parseString(requireNotNull(response.body).string()).asJsonObject
    }

    private fun checkedName(file: JsonObject): String = requireNotNull(file.get("name")?.asString).also {
        require(it.matches(Regex("files/(?:[a-z0-9]|[a-z0-9][a-z0-9-]{0,38}[a-z0-9])"))) { "Unerwarteter Google-Dateiname." }
    }

    private fun authorized(builder: Request.Builder, auth: GeminiRequestAuth): Request.Builder = when (auth) {
        is GeminiRequestAuth.ApiKey -> builder.header("x-goog-api-key", auth.apiKey)
        is GeminiRequestAuth.OAuth -> builder.header("Authorization", "Bearer ${auth.accessToken}")
    }

    private fun sameOrigin(url: HttpUrl, origin: HttpUrl) = url.scheme == origin.scheme && url.host == origin.host &&
        url.port == origin.port && url.username.isEmpty() && url.password.isEmpty()

    companion object {
        fun create(client: OkHttpClient, cacheDir: File): GeminiFileUploader {
            val bounded = client.newBuilder().followRedirects(false)
                .readTimeout(30, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).build()
            val downloads = bounded.newBuilder().dns(PublicVideoDns(client.dns)).build()
            val cleanup = bounded.newBuilder().callTimeout(5, TimeUnit.SECONDS).build()
            return GeminiFileUploader(downloads, bounded, cacheDir, cleanupApi = cleanup)
        }
    }
}

internal fun geminiUploadHttpFailure(code: Int, response: String?): Exception {
    val reason = runCatching {
        JsonParser.parseString(response).asJsonObject.getAsJsonObject("error")
            ?.getAsJsonArray("details")?.mapNotNull { it.asJsonObject.get("reason")?.asString }
            ?.firstOrNull { it.matches(Regex("[A-Z_]{1,80}")) }
    }.getOrNull()
    val explanation = if (reason == "ACCESS_TOKEN_SCOPE_INSUFFICIENT") {
        "Google-Berechtigung fuer Datei-Uploads fehlt. YouTube benoetigt diese Zusatzberechtigung nicht."
    } else "Video-Upload konnte nicht gestartet werden."
    return IllegalArgumentException("$explanation (HTTP $code${reason?.let { ", $it" }.orEmpty()}).")
}

internal class PublicVideoDns(private val delegate: Dns) : Dns {
    override fun lookup(hostname: String): List<InetAddress> = delegate.lookup(hostname).also { addresses ->
        if (addresses.isEmpty() || addresses.any { address ->
                val bytes = address.address
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                address.isAnyLocalAddress || address.isLoopbackAddress || address.isSiteLocalAddress ||
                    address.isLinkLocalAddress || address.isMulticastAddress ||
                    (bytes.size == 4 && (first == 0 || first >= 224 ||
                        (first == 100 && second in 64..127) || (first == 198 && second in 18..19))) ||
                    (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc)
            }) throw UnknownHostException("Private Video-Server werden nicht heruntergeladen.")
    }
}
