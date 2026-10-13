package com.recipegrabber

import com.recipegrabber.data.remote.DriveBackupTransport
import com.recipegrabber.domain.backup.RecipeBackupCodec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DriveBackupTransportTest {
    private class Endpoint(vararg bodies: String, private val code: Int = 200, private val body: ResponseBody? = null) : Call.Factory {
        val replies = ArrayDeque(bodies.toList())
        val requests = mutableListOf<Request>()
        val payloads = mutableListOf<String>()
        override fun newCall(request: Request): Call {
            requests += request
            payloads += Buffer().also { request.body?.writeTo(it) }.readUtf8()
            val response = Response.Builder().request(request).protocol(Protocol.HTTP_2).code(code).message("fixture")
                .body(body ?: replies.removeFirst().toResponseBody()).build()
            val call = mockk<Call>(relaxed = true)
            every { call.enqueue(any()) } answers { firstArg<Callback>().onResponse(call, response) }
            return call
        }
    }

    private val file = """{"id":"fixture_1","createdTime":"2026-10-13T12:00:00Z","appProperties":{"recipegrabberFormat":"1","recipeCount":"3"}}"""

    @Test fun `multipart creates private immutable snapshot without credentials in archive`() = runTest {
        val endpoint = Endpoint("""{"id":"fixture_1"}""")
        assertEquals("fixture_1", DriveBackupTransport(endpoint).upload("synthetic-token", RecipeBackupCodec.encode(emptyList()), 0))
        val request = endpoint.requests.single()
        assertEquals("www.googleapis.com", request.url.host)
        assertEquals("multipart", request.url.queryParameter("uploadType"))
        assertEquals("POST", request.method)
        assertEquals("Bearer synthetic-token", request.header("Authorization"))
        val body = endpoint.payloads.single()
        assertTrue(body.contains("appDataFolder") && body.contains("recipegrabberFormat"))
        assertFalse(body.contains("synthetic-token"))
    }

    @Test fun `listing paginates only own format and rejects unsupported metadata`() = runTest {
        val endpoint = Endpoint("""{"files":[$file],"nextPageToken":"page2"}""", """{"files":[$file,${file.replace("\"1\",\"recipeCount\"", "\"2\",\"recipeCount\"")}] }""")
        assertEquals(1, DriveBackupTransport(endpoint).list("synthetic-token").size)
        assertEquals("appDataFolder", endpoint.requests.first().url.queryParameter("spaces"))
        assertEquals("page2", endpoint.requests.last().url.queryParameter("pageToken"))
        assertTrue(endpoint.requests.all { it.method == "GET" && it.url.host == "www.googleapis.com" })
    }

    @Test fun `file identifiers cannot change request origin or path`() = runTest {
        val endpoint = Endpoint()
        for (id in listOf("../secret", "https://evil.example/", "a?alt=other", "")) {
            assertTrue(runCatching { DriveBackupTransport(endpoint).download("synthetic-token", id) }.isFailure)
        }
        assertTrue(endpoint.requests.isEmpty())
    }

    @Test fun `failed and redirected requests do not expose response body`() = runTest {
        for (code in listOf(302, 401, 403, 500)) {
            val endpoint = Endpoint("sensitive-error-content", code = code)
            val error = runCatching { DriveBackupTransport(endpoint).download("synthetic-token", "fixture") }.exceptionOrNull()
            assertNotNull(error)
            assertFalse(error!!.message.orEmpty().contains("sensitive-error-content"))
            assertEquals(1, endpoint.requests.size)
        }
    }

    @Test fun `unknown content length is bounded during streaming`() = runTest {
        val body = object : ResponseBody() {
            private val buffer = Buffer().write(ByteArray(RecipeBackupCodec.MAX_BYTES + 1))
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = buffer
        }
        assertTrue(runCatching { DriveBackupTransport(Endpoint(body = body)).download("synthetic-token", "fixture") }.isFailure)
    }

    @Test fun `repeated pagination fails instead of silently truncating backups`() = runTest {
        val endpoint = Endpoint("""{"files":[],"nextPageToken":"repeat"}""", """{"files":[],"nextPageToken":"repeat"}""")
        assertTrue(runCatching { DriveBackupTransport(endpoint).list("synthetic-token") }.isFailure)
    }

    @Test fun `cancellation cancels in flight network call`() = runTest {
        val call = mockk<Call>(relaxed = true)
        val calls = Call.Factory { call }
        val job = async(start = CoroutineStart.UNDISPATCHED) { DriveBackupTransport(calls).download("synthetic-token", "fixture") }
        job.cancel(); job.join()
        assertTrue(job.isCancelled)
        verify(exactly = 1) { call.cancel() }
    }
}
