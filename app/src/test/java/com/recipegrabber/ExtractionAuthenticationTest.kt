package com.recipegrabber

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.apify.ApifyService
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.GeminiAuthMode
import com.recipegrabber.domain.llm.LlmProvider
import com.recipegrabber.domain.llm.LlmProviderFactory
import com.recipegrabber.domain.llm.ProviderType
import com.recipegrabber.domain.usecase.ExtractRecipeUseCase
import com.recipegrabber.domain.usecase.SaveRecipeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExtractionAuthenticationTest {
    private val preferences = mockk<PreferencesRepository>()
    private val provider = mockk<LlmProvider>()
    private val factory = mockk<LlmProviderFactory>()
    private val save = mockk<SaveRecipeUseCase>()
    private val useCase = ExtractRecipeUseCase(
        mockk<ApifyService>(), factory, preferences, save, mockk<AppLogger>(relaxed = true)
    )
    private val url = "https://example.com/video"

    private fun configure(mode: GeminiAuthMode) {
        every { preferences.apifyApiKey } returns flowOf("")
        every { preferences.llmProviderType } returns flowOf(ProviderType.GEMINI)
        every { preferences.geminiAuthMode } returns flowOf(mode)
        every { preferences.geminiApiKey } returns flowOf("")
        every { factory.create(ProviderType.GEMINI) } returns provider
    }

    @Test
    fun `oauth without api key reaches provider and preserves auth error`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        coEvery { provider.extractRecipeFromVideo(url) } returns
            Result.failure(Exception("OAuth registration missing"))
        val result = useCase.invokeDirect(url)
        assertEquals(ExtractRecipeUseCase.ExtractionResult.Error("OAuth registration missing"), result)
        coVerify(exactly = 1) { provider.extractRecipeFromVideo(url) }
        coVerify(exactly = 0) { save(any()) }
    }

    @Test
    fun `api key mode without key stops before provider`() = runTest {
        configure(GeminiAuthMode.API_KEY)
        assertTrue(useCase.invokeDirect(url) is ExtractRecipeUseCase.ExtractionResult.NoApiKey)
        coVerify(exactly = 0) { provider.extractRecipeFromVideo(any()) }
        coVerify(exactly = 0) { save(any()) }
    }

    @Test
    fun `empty model output is not saved as recipe`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        coEvery { provider.extractRecipeFromVideo(url) } returns Result.success(Recipe(title = "Extracted Recipe"))
        coEvery { save(any()) } returns 1L
        assertTrue(useCase.invokeDirect(url) is ExtractRecipeUseCase.ExtractionResult.Error)
        coVerify(exactly = 0) { save(any()) }
    }

    @Test
    fun `oauth success saves and returns recipe details`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        val recipe = Recipe(title = "Suppe").also {
            it.ingredients = listOf(Ingredient(0, 0, "Kartoffeln", 200.0, "g", "", 0))
            it.steps = listOf(Step(0, 0, 1, "Kochen", null, null))
        }
        coEvery { provider.extractRecipeFromVideo(url) } returns Result.success(recipe)
        coEvery { save(recipe) } returns 42L
        val result = useCase.invokeDirect(url) as ExtractRecipeUseCase.ExtractionResult.Success
        assertEquals(42L, result.recipe.id)
        assertEquals(recipe.ingredients, result.recipe.ingredients)
        assertEquals(recipe.steps, result.recipe.steps)
        coVerify(exactly = 1) { save(recipe) }
    }

    @Test
    fun `cancellation returned by provider propagates`() {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        coEvery { provider.extractRecipeFromVideo(url) } returns
            Result.failure(CancellationException("cancelled"))
        assertThrows(CancellationException::class.java) {
            runTest { useCase.invokeDirect(url) }
        }
        coVerify(exactly = 0) { save(any()) }
    }

    @Test
    fun `cancelled extraction propagates cancellation`() {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        coEvery { provider.extractRecipeFromVideo(url) } throws CancellationException("cancelled")
        assertThrows(CancellationException::class.java) {
            runTest { useCase.invokeDirect(url) }
        }
        coVerify(exactly = 0) { save(any()) }
    }
}
