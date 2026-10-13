package com.recipegrabber

import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.domain.usecase.ExtractRecipeUseCase
import com.recipegrabber.domain.usecase.ProgressUpdate
import com.recipegrabber.presentation.viewmodel.ExtractionStep
import com.recipegrabber.presentation.viewmodel.RecipeExtractionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeExtractionLifecycleTest {
    private val extraction = mockk<ExtractRecipeUseCase>()
    private lateinit var model: RecipeExtractionViewModel
    private var cancelled = 0

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { extraction.progress } returns MutableStateFlow(ProgressUpdate("analyzing", "Analyze", 0.5f))
        coEvery { extraction(any()) } coAnswers {
            try { awaitCancellation() } finally { cancelled++ }
        }
        model = RecipeExtractionViewModel(extraction, mockk<AppLogger>(relaxed = true))
    }

    @AfterEach
    fun cleanup() {
        model.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `dismiss cancels extraction instead of only clearing the display`() {
        model.startExtraction("https://youtu.be/first")
        model.reset()
        assertEquals(1, cancelled)
        assertEquals(ExtractionStep.IDLE, model.uiState.value.step)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test
    fun `new URL cancels previous extraction before starting the replacement`() {
        model.startExtraction("https://youtu.be/first")
        model.startExtraction("https://youtu.be/second")
        assertEquals(1, cancelled)
        assertEquals("https://youtu.be/second", model.uiState.value.videoUrl)
        model.reset()
        assertEquals(2, cancelled)
    }

    @Test
    fun `failed direct fallback shows the actual provider error`() {
        coEvery { extraction(any()) } returns ExtractRecipeUseCase.ExtractionResult.ScrapingFailed("Scraping failed")
        coEvery { extraction.invokeDirect(any()) } returns ExtractRecipeUseCase.ExtractionResult.Error("Unsupported video input")
        model.startExtraction("https://tiktok.com/@cook/video/123")
        assertEquals("Unsupported video input", model.uiState.value.error)
        assertEquals(ExtractionStep.ERROR, model.uiState.value.step)
        assertFalse(model.uiState.value.isLoading)
    }
}
