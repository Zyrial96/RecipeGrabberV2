package com.recipegrabber

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipegrabber.data.local.RecipeDatabase
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.Step
import com.recipegrabber.data.repository.RecipeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipePersistenceTest {
    private lateinit var database: RecipeDatabase
    private lateinit var repository: RecipeRepository
    private val ingredients = listOf(Ingredient(id = 9, recipeId = 0, name = "Wasser", amount = 100.0, unit = "ml"))
    private val steps = listOf(Step(id = 9, recipeId = 0, order = 1, instruction = "Kochen"))

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java).build()
        repository = RecipeRepository(database.recipeDao(), database.ingredientDao(), database.stepDao())
    }

    @After fun close() { database.close() }

    @Test fun twoRecipesKeepIndependentDetails() = runBlocking {
        val first = repository.insertRecipeWithDetails(Recipe(title = "Suppe"), ingredients, steps)
        val second = repository.insertRecipeWithDetails(Recipe(title = "Tee"), ingredients, steps)
        val a = repository.getRecipeWithDetails(first).first()!!
        val b = repository.getRecipeWithDetails(second).first()!!
        assertEquals(1, a.ingredients.size)
        assertEquals(1, a.steps.size)
        assertNotEquals(a.ingredients.single().id, b.ingredients.single().id)
        assertNotEquals(a.steps.single().id, b.steps.single().id)
        assertEquals(first, a.ingredients.single().recipeId)
        assertEquals(second, b.steps.single().recipeId)
    }

    private fun rejectStepWrites() {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_steps BEFORE INSERT ON steps BEGIN SELECT RAISE(ABORT, 'test write failure'); END"
        )
    }

    @Test fun failedInsertRollsBackRecipeAndIngredients() = runBlocking {
        rejectStepWrites()
        assertTrue(runCatching {
            repository.insertRecipeWithDetails(Recipe(title = "Must not persist"), ingredients, steps)
        }.isFailure)
        assertTrue(repository.getAllRecipes().first().isEmpty())
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ingredients").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test fun failedUpdatePreservesOriginalAggregate() = runBlocking {
        val id = repository.insertRecipeWithDetails(Recipe(title = "Original"), ingredients, steps)
        val original = repository.getRecipeWithDetails(id).first()!!
        rejectStepWrites()
        assertTrue(runCatching {
            repository.updateRecipeWithDetails(original.recipe.copy(title = "Changed"), emptyList(), steps)
        }.isFailure)
        val after = repository.getRecipeWithDetails(id).first()!!
        assertEquals(original.recipe, after.recipe)
        assertEquals(original.ingredients, after.ingredients)
        assertEquals(original.steps, after.steps)
    }

    @Test fun favoriteAndDeletePreserveCascadeContract() = runBlocking {
        val id = repository.insertRecipeWithDetails(Recipe(title = "Suppe"), ingredients, steps)
        repository.toggleFavorite(id, true)
        assertEquals(id, repository.getFavoriteRecipes().first().single().id)
        repository.deleteRecipeById(id)
        assertNull(repository.getRecipeWithDetails(id).first())
        assertTrue(repository.getIngredientsForRecipe(id).isEmpty())
        assertTrue(repository.getStepsForRecipe(id).isEmpty())
    }
}
