package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.local.entity.Ingredient
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.local.entity.Step
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val scaledServings: Int = 0,
    val scaleFactor: Float = 1f
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: Long = checkNotNull(savedStateHandle["recipeId"])

    private val _scaledServings = MutableStateFlow(0)
    private val _scaleFactor = MutableStateFlow(1f)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecipeDetailUiState> = recipeRepository
        .getRecipeWithDetails(recipeId)
        .map { recipeWithDetails ->
            val servings = recipeWithDetails?.recipe?.servings ?: 4
            RecipeDetailUiState(
                recipe = recipeWithDetails,
                isLoading = false,
                scaledServings = (_scaleFactor.value * servings).toInt(),
                scaleFactor = _scaleFactor.value
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecipeDetailUiState(isLoading = true)
        )

    fun setScaleFactor(factor: Float) {
        _scaleFactor.value = factor
        val originalServings = uiState.value.recipe?.recipe?.servings ?: 4
        _scaledServings.value = (factor * originalServings).toInt()
    }

    fun incrementServings() {
        val currentServings = _scaledServings.value
        val originalServings = uiState.value.recipe?.recipe?.servings ?: 4
        val newFactor = (currentServings + 1).toFloat() / originalServings
        setScaleFactor(newFactor.coerceIn(0.5f, 10f))
    }

    fun decrementServings() {
        val currentServings = _scaledServings.value
        val originalServings = uiState.value.recipe?.recipe?.servings ?: 4
        val newFactor = (currentServings - 1).toFloat() / originalServings
        setScaleFactor(newFactor.coerceIn(0.5f, 10f))
    }

    fun scaleAmount(amount: Double?): Double? {
        return amount?.times(_scaleFactor.value)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            uiState.value.recipe?.recipe?.let { recipe ->
                try {
                    recipeRepository.toggleFavorite(recipe.id, !recipe.isFavorite)
                } catch (e: Exception) {
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
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
