package com.recipegrabber

import androidx.lifecycle.SavedStateHandle
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.RecipeRepository
import com.recipegrabber.presentation.viewmodel.RecipeDetailViewModel
import com.recipegrabber.presentation.util.formatIngredientAmount
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class PortionScalingTest {
    private val repository = mockk<RecipeRepository>()
    @AfterEach fun cleanup() { Dispatchers.resetMain() }

    private fun TestScope.model(servings: Int): RecipeDetailViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { repository.getRecipeWithDetails(1) } returns flowOf(
            RecipeWithDetails(Recipe(id = 1, title = "Suppe", servings = servings), emptyList(), emptyList())
        )
        val model = RecipeDetailViewModel(repository, mockk<AppLogger>(relaxed = true), SavedStateHandle(mapOf("recipeId" to 1L)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        runCurrent()
        return model
    }

    @Test fun `six portions decrement to exactly five`() = runTest {
        val model = model(6)
        model.decrementServings()
        runCurrent()
        assertEquals(5, model.uiState.value.scaledServings)
        assertEquals(500.0, model.scaleAmount(600.0)!!, 0.001)
    }

    @Test fun `large recipes can scale down to one portion`() = runTest {
        val model = model(20)
        repeat(30) { model.decrementServings() }
        runCurrent()
        assertEquals(1, model.uiState.value.scaledServings)
    }

    @Test fun `rapid increments are not lost and stop at ten times the original`() = runTest {
        val model = model(3)
        repeat(100) { model.incrementServings() }
        runCurrent()
        assertEquals(30, model.uiState.value.scaledServings)
    }

    @Test fun `legacy zero servings never divide by zero`() = runTest {
        val model = model(0)
        assertEquals(1, model.uiState.value.scaledServings)
        assertTrue(model.uiState.value.scaleFactor.isFinite())
    }

    @Test fun `favorite failure is visible in the collected state`() = runTest {
        val model = model(4)
        coEvery { repository.toggleFavorite(1, true) } throws IllegalStateException("Write failed")
        model.toggleFavorite()
        runCurrent()
        assertEquals("Write failed", model.uiState.value.error)
        model.clearError()
        runCurrent()
        assertNull(model.uiState.value.error)
    }

    @Test fun `German decimal formatting never leaves trailing comma`() {
        assertEquals("1", formatIngredientAmount(1.001, Locale.GERMANY))
        assertEquals("1,5", formatIngredientAmount(1.5, Locale.GERMANY))
        assertEquals("1000,25", formatIngredientAmount(1000.25, Locale.GERMANY))
        assertEquals("1.5", formatIngredientAmount(1.5, Locale.US))
    }
}
