package com.recipegrabber

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recipegrabber.data.local.RecipeDatabase
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.RecipeRepository
import com.recipegrabber.presentation.ui.screens.RecipeDetailScreen
import com.recipegrabber.presentation.ui.theme.RecipeGrabberTheme
import com.recipegrabber.presentation.viewmodel.RecipeDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

class RecipeDetailScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: RecipeDatabase
    private lateinit var repository: RecipeRepository
    private val store = ViewModelStore()
    private lateinit var context: Context
    private var id = 0L
    private var navigated = false
    private lateinit var previousLocale: Locale

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        repository = RecipeRepository(database.recipeDao(), database.ingredientDao(), database.stepDao())
        id = runBlocking {
            repository.insertRecipeWithDetails(
                Recipe(title = "QA Suppe", servings = 6),
                listOf(Ingredient(recipeId = 0, name = "Wasser", amount = 600.6, unit = "ml")),
                listOf(Step(recipeId = 0, order = 1, instruction = "Testschritt: Wasser kochen"))
            )
        }
        val loggerContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = File(context.cacheDir, "recipegrabber-qa-logs").also { it.mkdirs() }
        }
        compose.runOnUiThread {
            val factory = viewModelFactory {
                initializer { RecipeDetailViewModel(repository, AppLogger(loggerContext), SavedStateHandle(mapOf("recipeId" to id))) }
            }
            val model = ViewModelProvider(store, factory)[RecipeDetailViewModel::class.java]
            compose.setContent {
                RecipeGrabberTheme(darkTheme = true) {
                    RecipeDetailScreen(onNavigateBack = { navigated = true }, viewModel = model)
                }
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("QA Suppe").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun cleanup() {
        compose.runOnUiThread { store.clear() }
        database.close()
        Locale.setDefault(previousLocale)
    }

    @Test fun portionsAndLocalizedIngredientAmountsRenderCorrectly() {
        compose.onNodeWithContentDescription("Weniger").performClick()
        compose.onNodeWithTag("servings-value").assertTextEquals("5")
        compose.onNodeWithText("500,5 ml Wasser").assertIsDisplayed()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "recipegrabber-qa-details.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun portionControlsReachOneWithoutGoingBelowIt() {
        repeat(10) { compose.onNodeWithContentDescription("Weniger").performClick() }
        compose.onNodeWithTag("servings-value").assertTextEquals("1")
        compose.onNodeWithContentDescription("Mehr").performClick()
        compose.onNodeWithTag("servings-value").assertTextEquals("2")
    }

    @Test fun favoriteButtonUpdatesTheStoredRecipe() {
        compose.onNodeWithContentDescription("Favorit").performClick()
        compose.waitUntil(5000) { runBlocking { repository.getRecipeWithDetails(id).first()?.recipe?.isFavorite == true } }
    }

    @Test fun deleteRequiresConfirmationAndNavigatesAfterPersistence() {
        compose.onNodeWithContentDescription("L\u00f6schen").performClick()
        compose.onNodeWithText("Abbrechen").performClick()
        assertNotNull(runBlocking { repository.getRecipeWithDetails(id).first() })
        assertFalse(navigated)
        compose.onNodeWithContentDescription("L\u00f6schen").performClick()
        compose.onNodeWithText("L\u00f6schen").performClick()
        compose.waitUntil(5000) { navigated }
        assertNull(runBlocking { repository.getRecipeWithDetails(id).first() })
    }
}
