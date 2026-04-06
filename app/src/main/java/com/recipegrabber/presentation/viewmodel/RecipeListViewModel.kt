package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.local.entity.RecipeWithDetails
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.data.repository.RecipeRepository
import com.recipegrabber.domain.llm.LlmProviderFactory
import com.recipegrabber.domain.llm.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<RecipeWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

data class ExtractionState(
    val isExtracting: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null
)

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val preferencesRepository: PreferencesRepository,
    private val llmProviderFactory: LlmProviderFactory
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _extractionState = MutableStateFlow(ExtractionState())

    val uiState: StateFlow<RecipeListUiState> = combine(
        recipeRepository.getAllRecipesWithDetails(),
        _searchQuery,
        _isLoading,
        _error
    ) { recipes, query, loading, error ->
        val filteredRecipes = if (query.isBlank()) {
            recipes
        } else {
            recipes.filter {
                it.recipe.title.contains(query, ignoreCase = true) ||
                        it.recipe.description.contains(query, ignoreCase = true)
            }
        }
        RecipeListUiState(
            recipes = filteredRecipes,
            isLoading = loading,
            error = error,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipeListUiState(isLoading = true)
    )

    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun extractRecipeFromUrl(url: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState(
                isExtracting = true,
                message = "Starting extraction..."
            )

            try {
                val providerType = preferencesRepository.llmProviderType.first()
                val provider = llmProviderFactory.create(providerType)

                _extractionState.value = ExtractionState(
                    isExtracting = true,
                    progress = 0.3f,
                    message = "Analyzing video content..."
                )

                val result = provider.extractRecipeFromVideo(url)

                result.fold(
                    onSuccess = { recipe ->
                        _extractionState.value = ExtractionState(
                            isExtracting = true,
                            progress = 0.8f,
                            message = "Saving recipe..."
                        )

                        val recipeId = recipeRepository.insertRecipeWithDetails(
                            recipe.copy(sourceUrl = url),
                            recipe.ingredients,
                            recipe.steps
                        )

                        _extractionState.value = ExtractionState(
                            isExtracting = false,
                            progress = 1f,
                            message = "Recipe saved successfully!"
                        )
                    },
                    onFailure = { exception ->
                        _extractionState.value = ExtractionState(
                            isExtracting = false,
                            error = exception.message ?: "Failed to extract recipe"
                        )
                    }
                )
            } catch (e: Exception) {
                _extractionState.value = ExtractionState(
                    isExtracting = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                recipeRepository.deleteRecipe(recipe)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            try {
                recipeRepository.toggleFavorite(recipe.id, !recipe.isFavorite)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun resetExtractionState() {
        _extractionState.value = ExtractionState()
    }
}
