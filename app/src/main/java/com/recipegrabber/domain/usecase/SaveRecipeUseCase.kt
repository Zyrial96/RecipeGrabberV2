package com.recipegrabber.domain.usecase

import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(recipe: Recipe): Long = withContext(Dispatchers.IO) {
        recipeRepository.insertRecipeWithDetails(
            recipe = recipe,
            ingredients = recipe.ingredients,
            steps = recipe.steps
        )
    }
}