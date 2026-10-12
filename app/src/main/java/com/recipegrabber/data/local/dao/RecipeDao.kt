package com.recipegrabber.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Step
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeById(id: Long): Flow<Recipe?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeWithDetails(id: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipesWithDetails(): Flow<List<RecipeWithDetails>>

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchRecipes(query: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE isSynced = 0")
    suspend fun getUnsyncedRecipes(): List<Recipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Insert
    suspend fun insertIngredients(ingredients: List<Ingredient>)

    @Insert
    suspend fun insertSteps(steps: List<Step>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteSteps(recipeId: Long)

    @Transaction
    suspend fun insertRecipeWithDetails(recipe: Recipe, ingredients: List<Ingredient>, steps: List<Step>): Long {
        val id = insertRecipe(recipe)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = id) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = id) })
        return id
    }

    @Transaction
    suspend fun updateRecipeWithDetails(recipe: Recipe, ingredients: List<Ingredient>, steps: List<Step>) {
        updateRecipe(recipe)
        deleteIngredients(recipe.id)
        deleteSteps(recipe.id)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = recipe.id) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = recipe.id) })
    }

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE recipes SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, isSynced: Boolean)
}
