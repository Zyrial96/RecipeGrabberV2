package com.recipegrabber

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.presentation.ui.screens.*
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VisualRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nestedCardActionsDoNotOpenTheRecipeAndDeletionIsBehindMenu() {
        var opened = 0
        var favorites = 0
        var deleted = 0
        compose.setContent {
            RecipeGrabberTheme(darkTheme = false) {
                RecipeCard(fixture(), { opened++ }, { favorites++ }, { deleted++ })
            }
        }
        compose.onNodeWithContentDescription("Favorite").performClick()
        assertEquals(1, favorites)
        assertEquals(0, opened)
        compose.onNodeWithText("L\u00f6schen").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rezeptaktionen").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithText("L\u00f6schen").performClick()
        assertEquals(1, deleted)
        assertEquals(0, opened)
    }

    @Test fun enlargedFontCardKeepsBothActionsInsideCompactWidth() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                RecipeGrabberTheme(darkTheme = true) {
                    Box(Modifier.width(320.dp).testTag("frame")) {
                        RecipeCard(fixture(), {}, {}, {})
                    }
                }
            }
        }
        val frame = compose.onNodeWithTag("frame").fetchSemanticsNode().boundsInRoot
        val favorite = compose.onNodeWithContentDescription("Favorite").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithContentDescription("Rezeptaktionen").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(favorite.left >= frame.left && menu.right <= frame.right + 1f)
        assertTrue(favorite.right <= menu.left + 1f)
        compose.onNodeWithText("1 Zutat").assertIsDisplayed()
        compose.onNodeWithText("1 Schritt").assertIsDisplayed()
    }

    @Test fun favoriteFiltersExposeSelectionAndCanSwitchBack() {
        compose.setContent {
            var selected by remember { mutableStateOf(false) }
            RecipeGrabberTheme { LibraryFilters(selected, { selected = it }, 4) }
        }
        compose.onNodeWithText("Alle").assertIsSelected()
        compose.onNodeWithText("Favoriten").performClick().assertIsSelected()
        compose.onNodeWithText("Alle").performClick().assertIsSelected()
    }

    @Test fun ingredientChecklistTogglesWithoutChangingItsQuantity() {
        compose.setContent {
            RecipeGrabberTheme {
                IngredientItem(Ingredient(id = 1, recipeId = 1, name = "Wasser", unit = "ml"), 500.0)
            }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("500 ml Wasser").assertIsDisplayed()
        compose.onNode(isToggleable()).performClick().assertIsOff()
    }

    @Test fun compactLargeFontPortionControlsStayUsable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                RecipeGrabberTheme(darkTheme = false) {
                    var portions by remember { mutableStateOf(4) }
                    Column(Modifier.width(280.dp)) {
                        RecipeHeader(4, portions, 10, 20, { portions++ }, { portions-- })
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Mehr").assertIsDisplayed().performClick()
        compose.onNodeWithTag("servings-value").assertTextEquals("5")
        compose.onNodeWithContentDescription("Weniger").performClick()
        compose.onNodeWithTag("servings-value").assertTextEquals("4")
    }

    @Test fun entireSettingsRowTogglesExactlyOnce() {
        var changes = 0
        compose.setContent {
            var checked by remember { mutableStateOf(false) }
            RecipeGrabberTheme {
                SettingsToggle(androidx.compose.material.icons.Icons.Default.DarkMode,
                    "Dark Mode", "Light theme enabled", checked, { checked = it; changes++ })
            }
        }
        compose.onNodeWithText("Dark Mode").performClick()
        compose.onNode(isToggleable()).assertIsOn()
        assertEquals(1, changes)
    }

    @Test fun successActionsRemainReachableAtLargeFontInCompactSheet() {
        var viewed = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                RecipeGrabberTheme(darkTheme = false) {
                    Column(Modifier.width(280.dp).height(480.dp).verticalScroll(rememberScrollState())) {
                        SuccessState(fixture().recipe, { viewed = true }, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Rezept ansehen").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(viewed)
        compose.onNodeWithText("Schlie\u00dfen").performScrollTo().assertIsDisplayed()
    }

    private fun fixture() = RecipeWithDetails(
        Recipe(id = 1, title = "Kartoffel-Tortilla mit frischen Kr\u00e4utern und knackigem Gem\u00fcse",
            description = "Ein langes Rezept mit einer vollst\u00e4ndigen Beschreibung f\u00fcr die kompakte Kochbuchansicht."),
        listOf(Ingredient(recipeId = 1, name = "Kartoffeln")),
        listOf(Step(recipeId = 1, order = 1, instruction = "Kartoffeln garen."))
    )
}
