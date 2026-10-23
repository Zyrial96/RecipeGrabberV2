package com.recipegrabber

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.recipegrabber.presentation.ui.screens.OriginalVideoButton
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import com.recipegrabber.presentation.util.originalVideoIntent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OriginalVideoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun viewActionUsesExactOriginalSourceIncludingTimestamp() {
        val source = "https://www.youtube.com/watch?v=yKUui_b7JO0&t=42"
        var opened: Intent? = null
        compose.setContent { RecipeGrabberTheme { OriginalVideoButton(source) { opened = it } } }
        compose.onNodeWithText("Originalvideo ansehen").assertIsDisplayed().performClick()
        assertEquals(Intent.ACTION_VIEW, opened?.action)
        assertEquals(source, opened?.dataString)
        assertTrue(opened!!.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test fun emptyAndUnsafeSourcesCannotCreateExternalIntents() {
        listOf("", "not a link", "http://example.com/video.mp4", "javascript:alert(1)",
            "intent://video#Intent;end", "file:///private/file", "https://user:password@example.com/video")
            .forEach { assertNull(originalVideoIntent(it)) }
        assertNotNull(originalVideoIntent("https://www.instagram.com/reel/example/"))
        assertNotNull(originalVideoIntent("https://example.com/cooking.webm"))
    }

    @Test fun missingSourceDoesNotOfferADeadButton() {
        compose.setContent { RecipeGrabberTheme { OriginalVideoButton("") { fail("No source must not open") } } }
        compose.onNodeWithText("Originalvideo ansehen").assertDoesNotExist()
    }
}
