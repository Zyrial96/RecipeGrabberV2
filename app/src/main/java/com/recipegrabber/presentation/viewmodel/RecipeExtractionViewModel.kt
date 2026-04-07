package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.domain.usecase.ExtractRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExtractionUiState(
    val isLoading: Boolean = false,
    val videoUrl: String = "",
    val extractedRecipe: Recipe? = null,
    val error: String? = null,
    val step: ExtractionStep = ExtractionStep.IDLE
)

enum class ExtractionStep {
    IDLE,
    SCRAPING,
    EXTRACTING,
    SUCCESS,
    ERROR
}

@HiltViewModel
class RecipeExtractionViewModel @Inject constructor(
    private val extractRecipeUseCase: ExtractRecipeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractionUiState())
    val uiState: StateFlow<ExtractionUiState> = _uiState.asStateFlow()

    fun startExtraction(videoUrl: String) {
        viewModelScope.launch {
            _uiState.value = ExtractionUiState(
                isLoading = true,
                videoUrl = videoUrl,
                step = ExtractionStep.SCRAPING
            )

            val result = extractRecipeUseCase(videoUrl)

            when (result) {
                is ExtractRecipeUseCase.ExtractionResult.Success -> {
                    _uiState.value = ExtractionUiState(
                        isLoading = false,
                        videoUrl = videoUrl,
                        extractedRecipe = result.recipe,
                        step = ExtractionStep.SUCCESS
                    )
                }
                is ExtractRecipeUseCase.ExtractionResult.NoApiKey -> {
                    _uiState.value = ExtractionUiState(
                        isLoading = false,
                        videoUrl = videoUrl,
                        error = "Please configure your ${result.provider} API key in Settings",
                        step = ExtractionStep.ERROR
                    )
                }
                is ExtractRecipeUseCase.ExtractionResult.ScrapingFailed -> {
                    // Try direct extraction without scraping
                    _uiState.value = ExtractionUiState(
                        isLoading = true,
                        videoUrl = videoUrl,
                        step = ExtractionStep.EXTRACTING
                    )
                    
                    val retryResult = extractRecipeUseCase(videoUrl)
                    when (retryResult) {
                        is ExtractRecipeUseCase.ExtractionResult.Success -> {
                            _uiState.value = ExtractionUiState(
                                isLoading = false,
                                videoUrl = videoUrl,
                                extractedRecipe = retryResult.recipe,
                                step = ExtractionStep.SUCCESS
                            )
                        }
                        else -> {
                            _uiState.value = ExtractionUiState(
                                isLoading = false,
                                videoUrl = videoUrl,
                                error = result.message,
                                step = ExtractionStep.ERROR
                            )
                        }
                    }
                }
                is ExtractRecipeUseCase.ExtractionResult.Error -> {
                    _uiState.value = ExtractionUiState(
                        isLoading = false,
                        videoUrl = videoUrl,
                        error = result.message,
                        step = ExtractionStep.ERROR
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = ExtractionUiState()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}