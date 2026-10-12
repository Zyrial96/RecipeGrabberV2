package com.recipegrabber

import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.recipegrabber.domain.llm.GeminiRequestFactory
import com.recipegrabber.domain.llm.GeminiResponseParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GeminiVideoTest {
    private val recipe = """{"title":"Suppe","servings":6,"ingredients":[{"name":"Wasser","amount":100,"unit":"ml"}],"steps":[{"instruction":"Kochen"}],"sourceUrl":"https://wrong.example"}"""
    private fun response(text: String, finish: String = "STOP"): String = JsonObject().apply {
        add("candidates", JsonArray().apply {
            add(JsonObject().apply {
                addProperty("finishReason", finish)
                add("content", JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("thought", true); addProperty("text", "Not recipe JSON") })
                        add(JsonObject().apply { addProperty("text", text) })
                    })
                })
            })
        })
    }.toString()

    @ParameterizedTest
    @ValueSource(strings = ["https://youtu.be/abc", "https://www.youtube.com/watch?v=abc", "https://youtube.com/shorts/abc"])
    fun `youtube is attached as media not only text`(url: String) {
        val body = JsonParser.parseString(GeminiRequestFactory.buildVideoBody(url, "Extract recipe")).asJsonObject
        val parts = body.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")
        assertEquals(url, parts[0].asJsonObject.getAsJsonObject("fileData").get("fileUri").asString)
        assertEquals("Extract recipe", parts[1].asJsonObject.get("text").asString)
    }

    @Test fun `mp4 media type is explicit`() {
        assertTrue(GeminiRequestFactory.buildVideoBody("https://cdn.example/video.mp4?token=example", "Extract").contains("video/mp4"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://youtube.com/watch?v=abc", "https://youtube.com.evil.example/watch?v=abc", "https://youtube.com/", "https://tiktok.com/@user/video/123", "not a URL", "https://key:secret@youtube.com/watch?v=abc"])
    fun `unreadable or unsafe video links are rejected`(url: String) {
        assertThrows(IllegalArgumentException::class.java) { GeminiRequestFactory.buildVideoBody(url, "Extract") }
    }

    @Test fun `parse actual recipe while ignoring thought parts and generated source`() {
        val parsed = GeminiResponseParser.parse(response(recipe), "https://youtu.be/abc")
        assertEquals("Suppe", parsed.title)
        assertEquals(6, parsed.servings)
        assertEquals("https://youtu.be/abc", parsed.sourceUrl)
        assertEquals(0L, parsed.ingredients.single().id)
        assertEquals(0L, parsed.steps.single().id)
    }

    @ParameterizedTest
    @ValueSource(strings = ["null", "{}", "not JSON", "{\"title\":\"Fake\",\"servings\":0}"])
    fun `invalid recipes do not become success placeholders`(text: String) {
        assertThrows(Exception::class.java) { GeminiResponseParser.parse(response(text), "https://youtu.be/abc") }
    }

    @Test fun `truncated responses are rejected even with valid json`() {
        assertThrows(IllegalArgumentException::class.java) { GeminiResponseParser.parse(response(recipe, "MAX_TOKENS"), "https://youtu.be/abc") }
    }
}
