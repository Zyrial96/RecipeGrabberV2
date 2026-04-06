package com.recipegrabber.data.repository

import com.recipegrabber.data.local.dao.IngredientDao
import com.recipegrabber.data.local.dao.RecipeDao
import com.recipegrabber.data.local.dao.StepDao
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
    private val ingredientDao: IngredientDao,
    private val stepDao: StepDao
) {

    fun getAllRecipes(): Flow<List<Recipe>> = recipeDao.getAllRecipes()

    fun getAllRecipesWithDetails(): Flow<List<RecipeWithDetails>> = recipeDao.getAllRecipesWithDetails()

    fun getRecipeById(id: Long): Flow<Recipe?> = recipeDao.getRecipeById(id)

    fun getRecipeWithDetails(id: Long): Flow<RecipeWithDetails?> = recipeDao.getRecipeWithDetails(id)

    fun searchRecipes(query: String): Flow<List<Recipe>> = recipeDao.searchRecipes(query)

    fun getFavoriteRecipes(): Flow<List<Recipe>> = recipeDao.getFavoriteRecipes()

    suspend fun getUnsyncedRecipes(): List<Recipe> = recipeDao.getUnsyncedRecipes()

    suspend fun insertRecipe(recipe: Recipe): Long = recipeDao.insertRecipe(recipe)

    suspend fun insertRecipeWithDetails(recipe: Recipe, ingredients: List<Ingredient>, steps: List<Step>): Long {
        val recipeId = recipeDao.insertRecipe(recipe)
        val ingredientsWithRecipeId = ingredients.map { it.copy(recipeId = recipeId) }
        val stepsWithRecipeId = steps.map { it.copy(recipeId = recipeId) }
        ingredientDao.insertIngredients(ingredientsWithRecipeId)
        stepDao.insertSteps(stepsWithRecipeId)
        return recipeId
    }

    suspend fun updateRecipe(recipe: Recipe) = recipeDao.updateRecipe(recipe)

    suspend fun updateRecipeWithDetails(recipe: Recipe, ingredients: List<Ingredient>, steps: List<Step>) {
        recipeDao.updateRecipe(recipe)
        ingredientDao.deleteIngredientsForRecipe(recipe.id)
        stepDao.deleteStepsForRecipe(recipe.id)
        ingredientDao.insertIngredients(ingredients.map { it.copy(recipeId = recipe.id) })
        stepDao.insertSteps(steps.map { it.copy(recipeId = recipe.id) })
    }

    suspend fun deleteRecipe(recipe: Recipe) = recipeDao.deleteRecipe(recipe)

    suspend fun deleteRecipeById(id: Long) = recipeDao.deleteRecipeById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = recipeDao.updateFavoriteStatus(id, isFavorite)

    suspend fun updateSyncStatus(id: Long, isSynced: Boolean) = recipeDao.updateSyncStatus(id, isSynced)

    suspend fun getIngredientsForRecipe(recipeId: Long): List<Ingredient> =
        ingredientDao.getIngredientsForRecipeSync(recipeId)

    suspend fun getStepsForRecipe(recipeId: Long): List<Step> =
        stepDao.getStepsForRecipeSync(recipeId)
}
