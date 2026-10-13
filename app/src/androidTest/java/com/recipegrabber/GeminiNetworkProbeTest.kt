package com.recipegrabber

import android.util.Log
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.auth.api.identity.Identity
import com.google.gson.JsonParser
import com.recipegrabber.data.remote.GoogleAiAuthorizationRequestFactory
import com.recipegrabber.domain.llm.GeminiRequestAuth
import com.recipegrabber.domain.llm.GeminiRequestFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class GeminiNetworkProbeTest {
    @Test
    fun minimalGenerationResponds() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("generationProbe") == "true")
        val project = arguments.getString("googleProject")
        require(project == null || project.matches(Regex("[a-z][a-z0-9-]{4,61}[a-z0-9]")))
        val http1 = arguments.getString("generationHttp1") == "true"
        val waitSeconds = arguments.getString("generationWaitSeconds")?.let {
            requireNotNull(it.toLongOrNull()) { "Invalid generation wait limit" }
        } ?: 45L
        require(waitSeconds in 10L..600L)
        val authorization = Identity.getAuthorizationClient(ApplicationProvider.getApplicationContext<Context>())
            .authorize(GoogleAiAuthorizationRequestFactory.buildGenerativeLanguageRequest()).await()
        assertTrue("Existing consent required; this probe does not launch consent", !authorization.hasResolution())
        val token = authorization.accessToken
        assertTrue("No access token returned", !token.isNullOrBlank())
        val request = GeminiRequestFactory.buildGenerateContentRequest(
            "gemini-3.8-flash",
            """{"contents":[{"parts":[{"text":"Return only this JSON object: {\"ok\":true}"}]}],"generationConfig":{"responseMimeType":"application/json","maxOutputTokens":1024}}""",
            GeminiRequestAuth.OAuth(requireNotNull(token))
        ).newBuilder().apply {
            if (project != null) header("x-goog-user-project", project)
        }.build()
        val client = OkHttpClient.Builder()
            .apply { if (http1) protocols(listOf(Protocol.HTTP_1_1)) }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(waitSeconds, TimeUnit.SECONDS)
            .callTimeout(minOf(waitSeconds + 15L, 600L), TimeUnit.SECONDS)
            .build()
        val started = System.nanoTime()
        try {
            client.newCall(request).execute().use { response ->
                val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                val body = JsonParser.parseString(response.body!!.string()).asJsonObject
                val error = body.getAsJsonObject("error")
                val status = error?.get("status")?.asString.orEmpty()
                val reasons = error?.getAsJsonArray("details")?.mapNotNull {
                    it.asJsonObject.get("reason")?.asString
                }.orEmpty().joinToString(",")
                Log.i("RG-NetworkProbe", "minimal generation: HTTP ${response.code}, status=$status, reasons=$reasons, quotaHeader=${project != null}, protocol=${response.protocol}, ${elapsed}ms")
                assertTrue("HTTP ${response.code}, $status, $reasons", response.isSuccessful)
                val candidate = body.getAsJsonArray("candidates").first().asJsonObject
                val output = candidate.getAsJsonObject("content").getAsJsonArray("parts")
                    .map { it.asJsonObject }
                    .filter { it.get("thought")?.asBoolean != true }
                    .mapNotNull { it.get("text")?.asString }.joinToString("")
                assertTrue("Expected synthetic JSON response", JsonParser.parseString(output).asJsonObject.get("ok").asBoolean)
            }
        } catch (error: Exception) {
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            Log.i("RG-NetworkProbe", "minimal generation: ${error.javaClass.simpleName}, quotaHeader=${project != null}, http1=$http1, ${elapsed}ms")
            throw error
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    @Test
    fun authorizedModelMetadataResponds() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("oauthProbe") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authorization = Identity.getAuthorizationClient(context)
            .authorize(GoogleAiAuthorizationRequestFactory.buildGenerativeLanguageRequest()).await()
        assertTrue("Existing consent required; this probe does not launch consent", !authorization.hasResolution())
        val token = authorization.accessToken
        assertTrue("No access token returned", !token.isNullOrBlank())
        val project = arguments.getString("googleProject")
        require(project == null || project.matches(Regex("[a-z][a-z0-9-]{4,61}[a-z0-9]")))
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
        val failures = mutableListOf<String>()
        try {
            for (quotaProject in if (project == null) listOf(null) else listOf(null, project)) {
                val label = if (quotaProject == null) "no quota header" else "explicit quota project"
                val builder = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash")
                    .header("Authorization", "Bearer $token")
                if (quotaProject != null) builder.header("x-goog-user-project", quotaProject)
                try {
                    client.newCall(builder.build()).execute().use { response ->
                        val body = JsonParser.parseString(response.body!!.string()).asJsonObject
                        val error = body.getAsJsonObject("error")
                        val status = error?.get("status")?.asString.orEmpty()
                        val reasons = error?.getAsJsonArray("details")?.mapNotNull {
                            it.asJsonObject.get("reason")?.asString
                        }.orEmpty().joinToString(",")
                        Log.i("RG-NetworkProbe", "$label: HTTP ${response.code}, status=$status, reasons=$reasons")
                        if (!response.isSuccessful) failures += "$label: HTTP ${response.code}, $status, $reasons"
                    }
                } catch (error: Exception) {
                    val detail = "$label: ${error.javaClass.simpleName}"
                    Log.i("RG-NetworkProbe", detail)
                    failures += detail
                }
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
        assertTrue(failures.joinToString("; "), failures.isEmpty())
    }

    @Test
    fun unauthenticatedEndpointRespondsOverBothProtocols() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("networkProbe") == "true")
        val failures = mutableListOf<String>()
        for (protocols in listOf(listOf(Protocol.HTTP_1_1), listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))) {
            val client = OkHttpClient.Builder()
                .protocols(protocols)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .build()
            val label = protocols.first().toString()
            val started = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models")
                    .build()
                client.newCall(request).execute().use { response ->
                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                    Log.i("RG-NetworkProbe", "$label: HTTP ${response.code}, negotiated ${response.protocol}, ${elapsed}ms")
                    if (response.code !in listOf(401, 403)) failures += "$label: unexpected HTTP ${response.code}"
                }
            } catch (error: Exception) {
                val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                val detail = "$label: ${error.javaClass.simpleName}, ${elapsed}ms"
                Log.i("RG-NetworkProbe", detail)
                failures += detail
            } finally {
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
        assertTrue(failures.joinToString("; "), failures.isEmpty())
    }
}
