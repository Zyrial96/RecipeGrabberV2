package com.recipegrabber

import com.recipegrabber.domain.llm.awaitGeminiResponse
import com.recipegrabber.domain.llm.consumeGeminiResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class GeminiHttpCallTest {
    private val callback = slot<Callback>()
    private val call = mockk<Call>(relaxed = true).also {
        every { it.enqueue(capture(callback)) } returns Unit
    }
    private val body = mockk<ResponseBody>(relaxed = true)
    private fun response() = Response.Builder()
        .request(Request.Builder().url("https://example.com").build())
        .protocol(Protocol.HTTP_2).code(200).message("OK").body(body).build()

    @Test
    fun successfulResponseIsOwnedByCaller() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitGeminiResponse() }
        val response = response()
        callback.captured.onResponse(call, response)
        assertSame(response, result.await())
        verify(exactly = 0) { body.close() }
        response.close()
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun cancellationStopsCallAndClosesLateResponse() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitGeminiResponse() }
        result.cancel()
        callback.captured.onResponse(call, response())
        result.join()
        assertTrue(result.isCancelled)
        verify(exactly = 1) { call.cancel() }
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun cancellationBeforeDispatchClosesDeliveredResponse() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitGeminiResponse() }
        callback.captured.onResponse(call, response())
        result.cancel()
        result.join()
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun networkFailureIsPropagated() = runTest {
        val error = IOException("offline")
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { call.awaitGeminiResponse() }
        }
        callback.captured.onFailure(call, error)
        val failure = result.await().exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(error.message, failure?.message)
    }

    @Test
    fun consumingResponseClosesBodyBeforeReturning() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.consumeGeminiResponse { "parsed" } }
        callback.captured.onResponse(call, response())
        assertEquals("parsed", result.await())
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun consumingFailureStillClosesBody() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { runCatching { call.consumeGeminiResponse { throw IOException("read failure") } } }
        callback.captured.onResponse(call, response())
        assertEquals("read failure", result.await().exceptionOrNull()?.message)
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun cancellationDuringConsumptionCancelsNetworkAndClosesBody() = runTest {
        lateinit var result: kotlinx.coroutines.Deferred<String>
        result = async(start = CoroutineStart.UNDISPATCHED) { call.consumeGeminiResponse { result.cancel(); "ignored" } }
        callback.captured.onResponse(call, response())
        result.join()
        assertTrue(result.isCancelled)
        verify(exactly = 1) { call.cancel() }
        verify(exactly = 1) { body.close() }
    }

    @Test
    fun lateFailureDoesNotReplaceCancellation() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { call.awaitGeminiResponse() }
        result.cancel()
        callback.captured.onFailure(call, IOException("cancelled"))
        result.join()
        assertTrue(result.isCancelled)
        verify(exactly = 1) { call.cancel() }
    }
}
