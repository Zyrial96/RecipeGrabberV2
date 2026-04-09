package com.recipegrabber.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: RecipeWithDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val scaledServings: Int = 4,
    val scaleFactor: Float = 1f
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val logger: AppLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: Long = checkNotNull(savedStateHandle["recipeId"])

    private val _scaleFactor = MutableStateFlow(1f)

    val uiState: StateFlow<RecipeDetailUiState> = recipeRepository
        .getRecipeWithDetails(recipeId)
        .map { recipeWithDetails ->
            val originalServings = recipeWithDetails?.recipe?.servings ?: 4
            val currentScale = _scaleFactor.value
            RecipeDetailUiState(
                recipe = recipeWithDetails,
                isLoading = false,
                scaledServings = (currentScale * originalServings).toInt(),
                scaleFactor = currentScale
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecipeDetailUiState(isLoading = true)
        )

    fun incrementServings() {
        val original = uiState.value.recipe?.recipe?.servings ?: 4
        val current = uiState.value.scaledServings
        val newServings = current + 1
        val newFactor = newServings.toFloat() / original
        if (newFactor <= 10f) {
            _scaleFactor.value = newFactor
            logger.d("RecipeDetail", "Servings increased to $newServings (factor: ${String.format("%.1f", newFactor)})")
        }
    }

    fun decrementServings() {
        val original = uiState.value.recipe?.recipe?.servings ?: 4
        val current = uiState.value.scaledServings
        val newServings = current - 1
        if (newServings >= 1) {
            val newFactor = newServings.toFloat() / original
            if (newFactor >= 0.25f) {
                _scaleFactor.value = newFactor
                logger.d("RecipeDetail", "Servings decreased to $newServings (factor: ${String.format("%.1f", newFactor)})")
            }
        }
    }

    fun scaleAmount(amount: Double?): Double? {
        return amount?.times(_scaleFactor.value)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            uiState.value.recipe?.recipe?.let { recipe ->
                try {
                    recipeRepository.toggleFavorite(recipe.id, !recipe.isFavorite)
                    logger.i("RecipeDetail", "Toggled favorite for recipe: ${recipe.title}")
                } catch (e: Exception) {
                    logger.e("RecipeDetail", "Failed to toggle favorite", e)
                    _error.value = e.message
                }
            }
        }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            try {
                uiState.value.recipe?.recipe?.let { recipe ->
                    recipeRepository.deleteRecipe(recipe)
                    logger.i("RecipeDetail", "Deleted recipe: ${recipe.title}")
                }
            } catch (e: Exception) {
                logger.e("RecipeDetail", "Failed to delete recipe", e)
                _error.value = e.message
            }
        }
    }

    fun shareRecipe(context: Context) {
        val recipe = uiState.value.recipe ?: return
        val r = recipe.recipe
        val originalServings = r.servings
        val scaleFactor = _scaleFactor.value

        val sb = StringBuilder()
        sb.appendLine("🍳 ${r.title}")
        sb.appendLine()
        if (r.description.isNotBlank()) {
            sb.appendLine(r.description)
            sb.appendLine()
        }
        sb.appendLine("Portionen: ${uiState.value.scaledServings} (original: $originalServings)")
        sb.appendLine("Vorbereitung: ${r.prepTimeMinutes} Min | Kochzeit: ${r.cookTimeMinutes} Min")
        sb.appendLine()
        sb.appendLine("📋 Zutaten:")
        recipe.ingredients.sortedBy { it.orderIndex }.forEach { ing ->
            val scaledAmt = ing.amount?.times(scaleFactor)
            val amtStr = if (scaledAmt != null && scaledAmt > 0) {
                val formatted = if (scaledAmt == scaledAmt.toLong().toDouble()) {
                    scaledAmt.toLong().toString()
                } else {
                    String.format("%.2f", scaledAmt).trimEnd('0').trimEnd('.')
                }
                "$formatted ${ing.unit}"
            } else ""
            val name = ing.name
            val notes = if (ing.notes.isNotBlank()) " (${ing.notes})" else ""
            sb.appendLine("• $amtStr $name$notes")
        }
        sb.appendLine()
        sb.appendLine("📝 Zubereitung:")
        recipe.steps.sortedBy { it.order }.forEachIndexed { idx, step ->
            sb.appendLine("${idx + 1}. ${step.instruction}")
            if (step.duration != null && step.duration > 0) {
                sb.appendLine("   ⏱ ${step.duration} Min.")
            }
        }
        if (r.sourceUrl.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("Quelle: ${r.sourceUrl}")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Rezept teilen")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
        logger.i("RecipeDetail", "Shared recipe: ${r.title}")
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private val _error = MutableStateFlow<String?>(null)
    }
}