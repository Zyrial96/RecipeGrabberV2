package com.recipegrabber

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.apify.ApifyService
import com.recipegrabber.data.remote.apify.ScrapedVideoData
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
    private val apify = mockk<ApifyService>()
    private val useCase = ExtractRecipeUseCase(
        apify, factory, preferences, save, mockk<AppLogger>(relaxed = true)
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
        coEvery { save(any()) } returns 42L
        val result = useCase.invokeDirect(url) as ExtractRecipeUseCase.ExtractionResult.Success
        assertEquals(42L, result.recipe.id)
        assertEquals(recipe.ingredients, result.recipe.ingredients)
        assertEquals(recipe.steps, result.recipe.steps)
        assertEquals(url, result.recipe.sourceUrl)
        coVerify(exactly = 1) { save(match { it.sourceUrl == url }) }
    }

    private fun validRecipe() = Recipe(title = "Suppe").also {
        it.ingredients = listOf(Ingredient(0, 0, "Wasser", 200.0, "ml", "", 0))
        it.steps = listOf(Step(0, 0, 1, "Kochen", 5, null))
    }

    @Test
    fun `invalid numeric values never reach persistence`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        val invalid = listOf(
            validRecipe().copy(servings = 0),
            validRecipe().copy(prepTimeMinutes = -1),
            validRecipe().copy(cookTimeMinutes = -1),
            validRecipe().also { it.ingredients = it.ingredients.map { i -> i.copy(amount = Double.NaN) } },
            validRecipe().also { it.ingredients = it.ingredients.map { i -> i.copy(amount = Double.POSITIVE_INFINITY) } },
            validRecipe().also { it.ingredients = it.ingredients.map { i -> i.copy(amount = -1.0) } },
            validRecipe().also { it.steps = it.steps.map { s -> s.copy(duration = -1) } }
        )
        for (recipe in invalid) {
            if (recipe.ingredients.isEmpty()) recipe.ingredients = validRecipe().ingredients
            if (recipe.steps.isEmpty()) recipe.steps = validRecipe().steps
            coEvery { provider.extractRecipeFromVideo(url) } returns Result.success(recipe)
            assertTrue(useCase.invokeDirect(url) is ExtractRecipeUseCase.ExtractionResult.Error)
        }
        coVerify(exactly = 0) { save(any()) }
    }

    @Test
    fun `scraped video retains original source and details`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        every { preferences.apifyApiKey } returns flowOf("fixture-key")
        val original = "https://www.instagram.com/reel/fixture/"
        val media = "https://example.com/video.mp4?signature=fixture"
        coEvery { apify.scrapeInstagramReel(original, "fixture-key") } returns Result.success(
            ScrapedVideoData(media, "Beschreibung", null, "INSTAGRAM")
        )
        val recipe = validRecipe().copy(sourceUrl = media, sourceType = "invented").also {
            it.ingredients = validRecipe().ingredients
            it.steps = validRecipe().steps
        }
        coEvery { provider.extractRecipeFromVideo(media) } returns Result.success(recipe)
        coEvery { save(any()) } returns 42L
        val result = useCase(original) as ExtractRecipeUseCase.ExtractionResult.Success
        assertEquals(original, result.recipe.sourceUrl)
        assertEquals("VIDEO", result.recipe.sourceType)
        assertEquals("Beschreibung", result.recipe.description)
        assertEquals(recipe.ingredients, result.recipe.ingredients)
        assertEquals(recipe.steps, result.recipe.steps)
        coVerify { save(match { it.sourceUrl == original && it.ingredients == recipe.ingredients }) }
    }

    @Test
    fun `platform text in query does not trigger scraping`() = runTest {
        configure(GeminiAuthMode.GOOGLE_OAUTH)
        every { preferences.apifyApiKey } returns flowOf("fixture-key")
        val disguised = "https://example.com/video.mp4?ref=instagram.com"
        coEvery { provider.extractRecipeFromVideo(disguised) } returns Result.failure(Exception("fixture"))
        assertEquals(ExtractRecipeUseCase.ExtractionResult.Error("fixture"), useCase(disguised))
        coVerify(exactly = 0) { apify.scrapeInstagramReel(any(), any()) }
    }

    @Test
    fun `invalid URLs fail before credentials scraping or provider`() = runTest {
        for (invalid in listOf("not a URL", "file:///video.mp4", "https://user:password@example.com/video.mp4")) {
            assertTrue(useCase(invalid) is ExtractRecipeUseCase.ExtractionResult.Error)
        }
        coVerify(exactly = 0) { provider.extractRecipeFromVideo(any()) }
        coVerify(exactly = 0) { save(any()) }
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
