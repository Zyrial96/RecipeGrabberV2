package com.recipegrabber

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonParser
import com.recipegrabber.domain.VideoLinkParser
import com.recipegrabber.domain.llm.GeminiRequestFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoInputTest {
    @Test
    fun sharedSupportedMediaBuildsActualVideoParts() {
        val inputs = mapOf(
            "https://youtube.com/shorts/example" to null,
            "https://cdn.example/recipe.mp4?signature=test" to "video/mp4",
            "https://cdn.example/recipe.webm" to "video/webm",
            "https://cdn.example/recipe.mov" to "video/quicktime"
        )
        for ((url, mime) in inputs) {
            val recognized = VideoLinkParser.findVideoUrl("Rezept\n$url")
            assertEquals(url, recognized)
            val body = JsonParser.parseString(GeminiRequestFactory.buildVideoBody(recognized!!, "Extract")).asJsonObject
            val media = body.getAsJsonArray("contents")[0].asJsonObject
                .getAsJsonArray("parts")[0].asJsonObject.getAsJsonObject("fileData")
            assertEquals(url, media.get("fileUri").asString)
            assertEquals(mime, media.get("mimeType")?.asString)
        }
    }

    @Test
    fun unsafeSharedInputsAreNotAccepted() {
        for (url in listOf("http://youtube.com/watch?v=example", "https://key:secret@youtube.com/watch?v=example", "https://youtube.com.evil.example/watch?v=example")) {
            assertNull(VideoLinkParser.findVideoUrl(url))
        }
    }
}
