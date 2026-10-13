package com.recipegrabber

import com.recipegrabber.domain.VideoLinkParser
import com.recipegrabber.domain.llm.GeminiRequestFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VideoLinkParserTest {
    @ParameterizedTest
    @ValueSource(strings = [
        "https://www.youtube.com/watch?v=abc", "https://youtu.be/abc",
        "https://youtube.com/shorts/abc", "https://youtube.com/live/abc",
        "https://youtube.com/embed/abc", "https://cdn.example/video.mp4?signature=test",
        "https://cdn.example/video.webm", "https://cdn.example/video.MOV"
    ])
    fun `recognize actual media links in shared text`(url: String) {
        assertEquals(url, VideoLinkParser.findVideoUrl("Mein Rezept\n$url\nViel Spass"))
        GeminiRequestFactory.buildVideoBody(url, "Extract")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "http://youtube.com/watch?v=abc", "https://key:secret@youtube.com/watch?v=abc",
        "https://youtube.com.evil.example/watch?v=abc", "https://example.com/?u=youtube.com/watch?v=abc",
        "https://youtube.com/", "https://youtube.com/watch", "not a URL"
    ])
    fun `reject unsafe links and lookalike hosts`(text: String) {
        assertNull(VideoLinkParser.findVideoUrl(text))
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://www.tiktok.com/@cook/video/123", "https://www.instagram.com/reel/123"])
    fun `retain social scraper entry points`(url: String) {
        assertEquals(url, VideoLinkParser.findVideoUrl(url))
    }
}
