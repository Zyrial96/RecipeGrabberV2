package com.recipegrabber.domain.usecase

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val logger: AppLogger
) {
    suspend operator fun invoke(recipe: Recipe): Long = withContext(Dispatchers.IO) {
        logger.i("SaveRecipe", "Saving recipe: ${recipe.title} with ${recipe.ingredients.size} ingredients, ${recipe.steps.size} steps")
        try {
            val id = recipeRepository.insertRecipeWithDetails(
                recipe = recipe,
                ingredients = recipe.ingredients,
                steps = recipe.steps
            )
            logger.i("SaveRecipe", "Recipe saved successfully with ID: $id")
            id
        } catch (e: Exception) {
            logger.e("SaveRecipe", "Failed to save recipe: ${recipe.title}", e)
            throw e
        }
    }
}