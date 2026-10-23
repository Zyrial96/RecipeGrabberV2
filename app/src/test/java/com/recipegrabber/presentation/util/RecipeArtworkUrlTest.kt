package com.recipegrabber.presentation.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RecipeArtworkUrlTest {
    private val id = "yKUui_b7JO0"
    private val expected = "https://i.ytimg.com/vi/$id/hqdefault.jpg"

    @Test fun existingHttpsArtworkWins() {
        assertEquals("https://images.example/recipe.jpg", recipeArtworkUrl("https://images.example/recipe.jpg", "https://youtu.be/$id"))
    }
    @Test fun supportedYouTubeFormsUseOnlyThePublicVideoId() {
        listOf("https://www.youtube.com/watch?v=$id&t=12", "https://youtu.be/$id?si=private-tracking",
            "https://m.youtube.com/shorts/$id", "https://youtube.com/live/$id", "https://youtube.com/embed/$id")
            .forEach { assertEquals(expected, recipeArtworkUrl(null, it)) }
    }
    @Test fun untrustedOrMalformedSourcesNeverBecomeImageRequests() {
        listOf("http://youtu.be/$id", "https://youtube.com.evil.example/watch?v=$id", "https://user:password@youtu.be/$id",
            "https://youtu.be/invalid", "https://youtu.be/$id/extra", "https://youtube.com/watch?v=%ZZ", "not a URL")
            .forEach { assertNull(recipeArtworkUrl(null, it)) }
    }
    @Test fun unsafeThumbnailFallsBackWithoutForwardingCredentials() {
        assertEquals(expected, recipeArtworkUrl("https://user:password@images.example/a.jpg", "https://youtu.be/$id"))
        assertNull(recipeArtworkUrl("file:///private/a.jpg", "https://example.com/recipe.mp4"))
    }
}
