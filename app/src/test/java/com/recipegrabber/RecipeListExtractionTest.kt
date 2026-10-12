package com.recipegrabber

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.repository.RecipeRepository
import com.recipegrabber.domain.usecase.ExtractRecipeUseCase
import com.recipegrabber.presentation.viewmodel.RecipeListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListExtractionTest {
    private val repository = mockk<RecipeRepository>()
    private val extraction = mockk<ExtractRecipeUseCase>()
    private val url = "https://example.com/video"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.getAllRecipesWithDetails() } returns flowOf(emptyList())
    }

    @AfterEach
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `list extraction delegates errors without saving directly`() {
        coEvery { extraction(url) } returns ExtractRecipeUseCase.ExtractionResult.Error("Invalid recipe")
        val model = RecipeListViewModel(repository, extraction)
        model.extractRecipeFromUrl(url)
        assertEquals("Invalid recipe", model.extractionState.value.error)
        assertFalse(model.extractionState.value.isExtracting)
        coVerify(exactly = 1) { extraction(url) }
        coVerify(exactly = 0) { repository.insertRecipeWithDetails(any(), any(), any()) }
    }

    @Test
    fun `already saved success is not saved twice`() {
        coEvery { extraction(url) } returns ExtractRecipeUseCase.ExtractionResult.Success(
            Recipe(id = 42, title = "Suppe")
        )
        val model = RecipeListViewModel(repository, extraction)
        model.extractRecipeFromUrl(url)
        assertEquals(1f, model.extractionState.value.progress)
        assertEquals(null, model.extractionState.value.error)
        coVerify(exactly = 0) { repository.insertRecipeWithDetails(any(), any(), any()) }
    }

    @Test
    fun `scraping retry delegates direct extraction and shows its error`() {
        coEvery { extraction(url) } returns ExtractRecipeUseCase.ExtractionResult.ScrapingFailed("Scraping")
        coEvery { extraction.invokeDirect(url) } returns ExtractRecipeUseCase.ExtractionResult.Error("OAuth")
        val model = RecipeListViewModel(repository, extraction)
        model.extractRecipeFromUrl(url)
        assertEquals("OAuth", model.extractionState.value.error)
        coVerify(exactly = 1) { extraction.invokeDirect(url) }
    }
}
