package com.recipegrabber

import com.recipegrabber.data.local.dao.IngredientDao
import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.local.dao.StepDao
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Repository Tests")
class RecipeRepositoryTest {

    private lateinit var recipeDao: RecipeDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var stepDao: StepDao
    private lateinit var repository: RecipeRepository

    private val testRecipe = Recipe(
        id = 1,
        title = "Test Recipe",
        description = "A test recipe description",
        servings = 4,
        prepTimeMinutes = 10,
        cookTimeMinutes = 20,
        sourceUrl = "https://example.com",
        sourceType = "VIDEO",
        thumbnailUrl = null,
        createdAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        recipeDao = mockk(relaxed = true)
        ingredientDao = mockk(relaxed = true)
        stepDao = mockk(relaxed = true)
        repository = RecipeRepository(recipeDao, ingredientDao, stepDao)
    }

    @Nested
    @DisplayName("Recipe Operations")
    inner class RecipeOperations {

        @Test
        @DisplayName("Should insert recipe successfully")
        fun `should insert recipe successfully`() = runTest {
            coEvery { recipeDao.insertRecipe(any()) } returns 1L

            val result = repository.insertRecipe(testRecipe)

            assertEquals(1L, result)
            coVerify { recipeDao.insertRecipe(testRecipe) }
        }

        @Test
        @DisplayName("Should get all recipes as flow")
        fun `should get all recipes as flow`() = runTest {
            coEvery { recipeDao.getAllRecipes() } returns flowOf(listOf(testRecipe))

            val recipes = repository.getAllRecipes().first()

            assertEquals(1, recipes.size)
            assertEquals("Test Recipe", recipes[0].title)
        }

        @Test
        @DisplayName("Should search recipes by query")
        fun `should search recipes by query`() = runTest {
            coEvery { recipeDao.searchRecipes("Test") } returns flowOf(listOf(testRecipe))

            val recipes = repository.searchRecipes("Test").first()

            assertEquals(1, recipes.size)
            assertTrue(recipes[0].title.contains("Test"))
        }

        @Test
        @DisplayName("Should toggle favorite status")
        fun `should toggle favorite status`() = runTest {
            coEvery { recipeDao.updateFavoriteStatus(any(), any()) } returns Unit

            repository.toggleFavorite(1L, true)

            coVerify { recipeDao.updateFavoriteStatus(1L, true) }
        }

        @Test
        @DisplayName("Should delete recipe by id")
        fun `should delete recipe by id`() = runTest {
            coEvery { recipeDao.deleteRecipeById(any()) } returns Unit

            repository.deleteRecipeById(1L)

            coVerify { recipeDao.deleteRecipeById(1L) }
        }
    }

    @Nested
    @DisplayName("Ingredient Operations")
    inner class IngredientOperations {

        @Test
        @DisplayName("Should insert ingredients with recipe id")
        fun `should insert ingredients with recipe id`() = runTest {
            val ingredients = listOf(
                Ingredient(id = 0, recipeId = 0, name = "Flour", amount = 2.0, unit = "cups", notes = "", orderIndex = 0),
                Ingredient(id = 0, recipeId = 0, name = "Sugar", amount = 1.0, unit = "cup", notes = "", orderIndex = 1)
            )

            coEvery { ingredientDao.insertIngredients(any()) } returns listOf(1L, 2L)

            repository.insertRecipeWithDetails(
                testRecipe,
                ingredients,
                emptyList()
            )

            coVerify { ingredientDao.insertIngredients(any()) }
        }
    }

    @Nested
    @DisplayName("Step Operations")
    inner class StepOperations {

        @Test
        @DisplayName("Should insert steps with recipe id")
        fun `should insert steps with recipe id`() = runTest {
            val steps = listOf(
                Step(id = 0, recipeId = 0, order = 1, instruction = "Mix dry ingredients", duration = null, imageUrl = null),
                Step(id = 0, recipeId = 0, order = 2, instruction = "Add wet ingredients", duration = null, imageUrl = null)
            )

            coEvery { stepDao.insertSteps(any()) } returns listOf(1L, 2L)

            repository.insertRecipeWithDetails(
                testRecipe,
                emptyList(),
                steps
            )

            coVerify { stepDao.insertSteps(any()) }
        }
    }
}
