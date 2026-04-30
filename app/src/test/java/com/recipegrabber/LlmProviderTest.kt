package com.recipegrabber

import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.domain.llm.ProviderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LLM Provider Tests")
class LlmProviderTest {

    @Nested
    @DisplayName("Recipe Parsing Tests")
    inner class RecipeParsingTests {

        @Test
        @DisplayName("Should create valid Recipe entity")
        fun `should create valid Recipe entity`() {
            val recipe = Recipe(
                id = 1,
                title = "Test Recipe",
                description = "A test recipe",
                servings = 4,
                prepTimeMinutes = 15,
                cookTimeMinutes = 30,
                sourceUrl = "https://example.com",
                sourceType = "VIDEO",
                thumbnailUrl = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isFavorite = false,
                isSynced = false
            )
            recipe.ingredients = emptyList()
            recipe.steps = emptyList()

            assertEquals("Test Recipe", recipe.title)
            assertEquals(4, recipe.servings)
            assertEquals(15, recipe.prepTimeMinutes)
            assertEquals(30, recipe.cookTimeMinutes)
        }

        @Test
        @DisplayName("Should create valid Ingredient entity")
        fun `should create valid Ingredient entity`() {
            val ingredient = Ingredient(
                id = 1,
                recipeId = 1,
                name = "Flour",
                amount = 2.0,
                unit = "cups",
                notes = "sifted",
                orderIndex = 0
            )

            assertEquals("Flour", ingredient.name)
            assertEquals(2.0, ingredient.amount)
            assertEquals("cups", ingredient.unit)
            assertEquals("sifted", ingredient.notes)
        }

        @Test
        @DisplayName("Should create valid Step entity")
        fun `should create valid Step entity`() {
            val step = Step(
                id = 1,
                recipeId = 1,
                order = 1,
                instruction = "Preheat oven to 350F",
                duration = 10,
                imageUrl = null
            )

            assertEquals(1, step.order)
            assertEquals("Preheat oven to 350F", step.instruction)
            assertEquals(10, step.duration)
        }
    }

    @Nested
    @DisplayName("ProviderType Tests")
    inner class ProviderTypeTests {

        @Test
        @DisplayName("Should have correct enum values")
        fun `should have correct enum values`() {
            assertEquals(4, ProviderType.entries.size)
            assertEquals(ProviderType.OPENAI, ProviderType.valueOf("OPENAI"))
            assertEquals(ProviderType.GEMINI, ProviderType.valueOf("GEMINI"))
            assertEquals(ProviderType.CLAUDE, ProviderType.valueOf("CLAUDE"))
            assertEquals(ProviderType.KIMI, ProviderType.valueOf("KIMI"))
        }
    }
}
