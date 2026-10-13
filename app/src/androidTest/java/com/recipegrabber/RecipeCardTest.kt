package com.recipegrabber

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.presentation.ui.screens.RecipeCard
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import org.junit.Rule
import org.junit.Test

class RecipeCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun persistedIngredientsAreCountedFromTheLoadedRelation() {
        val details = RecipeWithDetails(
            recipe = Recipe(id = 1, title = "QA Suppe"),
            ingredients = listOf(
                Ingredient(recipeId = 1, name = "Wasser"),
                Ingredient(recipeId = 1, name = "Salz")
            ),
            steps = listOf(Step(recipeId = 1, order = 1, instruction = "Kochen"))
        )
        render(details)
        compose.onNodeWithText("2 ingredients").assertIsDisplayed()
        compose.onNodeWithText("1 steps").assertIsDisplayed()
    }

    @Test fun transientIngredientsDoNotOverrideAnEmptyPersistedRelation() {
        val recipe = Recipe(id = 1, title = "QA Suppe").apply {
            ingredients = listOf(Ingredient(recipeId = 1, name = "Stale ingredient"))
        }
        render(RecipeWithDetails(recipe, ingredients = emptyList(), steps = emptyList()))
        compose.onNodeWithText("0 ingredients").assertIsDisplayed()
    }

    private fun render(details: RecipeWithDetails) {
        compose.setContent {
            RecipeGrabberTheme(darkTheme = true) {
                RecipeCard(details, onClick = {}, onFavoriteClick = {}, onDeleteClick = {})
            }
        }
    }
}
