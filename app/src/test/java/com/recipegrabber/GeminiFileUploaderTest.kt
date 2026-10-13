package com.recipegrabber

import com.google.gson.JsonParser
import com.recipegrabber.domain.llm.GeminiFileUploader
import com.recipegrabber.domain.llm.GeminiRequestAuth
import com.recipegrabber.domain.llm.PublicVideoDns
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException

class GeminiFileUploaderTest {
    @TempDir lateinit var cache: File
    private val source = "https://cdn.example/recipe.webm"
    private val auth = GeminiRequestAuth.OAuth("synthetic-token")
    private val uri = "https://generativelanguage.googleapis.com/v1beta/files/test-file"
    private fun dnsFor(address: InetAddress) = object : Dns {
        override fun lookup(hostname: String) = listOf(address)
    }
    private fun metadata(state: String) = """{"name":"files/test-file","uri":"$uri","state":"$state"}"""

    private data class Reply(val text: String = "", val code: Int = 200, val headers: Map<String, String> = emptyMap(), val body: ResponseBody? = null)
    private class Endpoint(vararg replies: Reply) : Call.Factory {
        val pending = ArrayDeque(replies.toList())
        val requests = mutableListOf<Request>()
        val payloads = mutableListOf<String>()
        override fun newCall(request: Request): Call {
            requests += request
            payloads += Buffer().also { request.body?.writeTo(it) }.readUtf8()
            val reply = pending.removeFirst()
            val response = Response.Builder().request(request).protocol(Protocol.HTTP_2).code(reply.code).message("Test")
                .body(reply.body ?: reply.text.toResponseBody()).apply { reply.headers.forEach { (name, value) -> header(name, value) } }.build()
            val call = mockk<Call>(relaxed = true)
            every { call.enqueue(any()) } answers { firstArg<Callback>().onResponse(call, response) }
            return call
        }
    }

    private fun api(state: String = "ACTIVE") = Endpoint(
        Reply(headers = mapOf("X-Goog-Upload-URL" to "https://generativelanguage.googleapis.com/upload/v1beta/files?upload_id=fixture")),
        Reply("""{"file":${metadata(state)}}"""),
        *if (state == "PROCESSING") arrayOf(Reply(metadata("ACTIVE")), Reply(code = 204)) else arrayOf(Reply(code = 204))
    )

    @Test fun `upload polling and cleanup preserve source prompt and isolate credentials`() = runTest {
        val downloads = Endpoint(Reply("video"))
        val api = api("PROCESSING")
        GeminiFileUploader(downloads, api, cache, pollMillis = 1).withVideoBody(source, "Original $source", auth) { body ->
            val parts = JsonParser.parseString(body).asJsonObject.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")
            assertEquals(uri, parts[0].asJsonObject.getAsJsonObject("fileData").get("fileUri").asString)
            assertEquals("video/webm", parts[0].asJsonObject.getAsJsonObject("fileData").get("mimeType").asString)
            assertEquals("Original $source", parts[1].asJsonObject.get("text").asString)
        }
        assertNull(downloads.requests.single().header("Authorization"))
        assertNull(downloads.requests.single().header("x-goog-api-key"))
        assertEquals("RecipeGrabberV2 (https://github.com/Zyrial96/RecipeGrabberV2)", downloads.requests.single().header("User-Agent"))
        assertTrue(api.requests.all { it.header("Authorization") == "Bearer synthetic-token" })
        assertEquals("video", api.payloads[1])
        assertEquals("upload, finalize", api.requests[1].header("X-Goog-Upload-Command"))
        assertEquals("DELETE", api.requests.last().method)
        assertEquals("/v1beta/files/test-file", api.requests.last().url.encodedPath)
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `youtube avoids download upload and cleanup entirely`() = runTest {
        val downloads = Endpoint()
        val api = Endpoint()
        GeminiFileUploader(downloads, api, cache).withVideoBody("https://youtu.be/example", "Extract", auth) {
            assertTrue(it.contains("https://youtu.be/example"))
        }
        assertTrue(downloads.requests.isEmpty() && api.requests.isEmpty())
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `reject oversized declared content before upload`() = runTest {
        val api = Endpoint()
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("large-video")), api, cache, maxBytes = 4).withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure && api.requests.isEmpty())
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `unknown length streams are still bounded`() = runTest {
        val chunked = object : ResponseBody() {
            private val buffer = Buffer().writeUtf8("large-video")
            override fun contentType() = "video/webm".toMediaType()
            override fun contentLength() = -1L
            override fun source() = buffer
        }
        val api = Endpoint()
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply(body = chunked)), api, cache, maxBytes = 4).withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure && api.requests.isEmpty())
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `redirect never downgrades to HTTP`() = runTest {
        val api = Endpoint()
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply(code = 302, headers = mapOf("Location" to "http://cdn.example/video.webm"))), api, cache)
                .withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure && api.requests.isEmpty())
    }

    @Test fun `untrusted upload origin cannot receive bearer token`() = runTest {
        val api = Endpoint(Reply(headers = mapOf("X-Goog-Upload-URL" to "https://evil.example/upload")))
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache).withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure)
        assertEquals(1, api.requests.size)
        assertEquals("generativelanguage.googleapis.com", api.requests.single().url.host)
    }

    @Test fun `failed processing does not reach inference and cleans own file`() = runTest {
        val api = api("FAILED")
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache).withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure)
        assertEquals("DELETE", api.requests.last().method)
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `inference failure still cleans uploaded file`() = runTest {
        val api = api()
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache).withVideoBody(source, "Extract", auth) { error("parse failure") }
        }
        assertTrue(result.isFailure)
        assertEquals("DELETE", api.requests.last().method)
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `API key also remains isolated from source origin`() = runTest {
        val api = api()
        val downloads = Endpoint(Reply("video"))
        GeminiFileUploader(downloads, api, cache).withVideoBody(source, "Extract", GeminiRequestAuth.ApiKey("synthetic-key")) { }
        assertNull(downloads.requests.single().header("x-goog-api-key"))
        assertTrue(api.requests.all { it.header("x-goog-api-key") == "synthetic-key" && it.header("Authorization") == null })
    }

    @ParameterizedTest
    @ValueSource(strings = ["127.0.0.1", "10.0.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "198.18.0.1", "::1", "fc00::1", "fe80::1"])
    fun `public media download DNS rejects private destinations`(address: String) {
        val dns = PublicVideoDns(dnsFor(InetAddress.getByName(address)))
        assertThrows(UnknownHostException::class.java) { dns.lookup("video.example") }
    }

    @Test fun `public media download DNS retains public results`() {
        val address = InetAddress.getByName("8.8.8.8")
        assertEquals(listOf(address), PublicVideoDns(dnsFor(address)).lookup("video.example"))
    }

    @Test fun `processing deadline cleans uploaded file without inference`() = runTest {
        val api = Endpoint(
            Reply(headers = mapOf("X-Goog-Upload-URL" to "https://generativelanguage.googleapis.com/upload/v1beta/files?upload_id=fixture")),
            Reply("""{"file":${metadata("PROCESSING")}}"""), Reply(code = 204)
        )
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache, preparationTimeoutMillis = 10, pollMillis = 100)
                .withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.isFailure)
        assertEquals("DELETE", api.requests.last().method)
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `cancellation after upload also cleans the owned file`() = runTest {
        val api = api()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache).withVideoBody(source, "Extract", auth) { awaitCancellation() }
        }
        result.cancel()
        result.join()
        assertTrue(result.isCancelled)
        assertEquals("DELETE", api.requests.last().method)
        assertEquals(0, cache.listFiles()!!.size)
    }

    @Test fun `upload permission failure does not send media or invoke generation`() = runTest {
        val api = Endpoint(Reply(code = 403))
        val result = runCatching {
            GeminiFileUploader(Endpoint(Reply("video")), api, cache).withVideoBody(source, "Extract", auth) { error("Unexpected inference") }
        }
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("HTTP 403"))
        assertEquals(1, api.requests.size)
        assertEquals(0, cache.listFiles()!!.size)
    }
}
