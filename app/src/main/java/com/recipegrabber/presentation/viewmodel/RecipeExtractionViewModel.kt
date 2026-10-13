package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.local.entity.Recipe
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.domain.usecase.ExtractRecipeUseCase
import com.recipegrabber.domain.usecase.ProgressUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class ExtractionUiState(
    val isLoading: Boolean = false,
    val videoUrl: String = "",
    val extractedRecipe: Recipe? = null,
    val error: String? = null,
    val step: ExtractionStep = ExtractionStep.IDLE,
    val progressMessage: String = "",
    val progressPercent: Float = 0f
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
    private val extractRecipeUseCase: ExtractRecipeUseCase,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractionUiState())
    val uiState: StateFlow<ExtractionUiState> = _uiState.asStateFlow()
    private var extractionJob: Job? = null

    fun startExtraction(videoUrl: String) {
        extractionJob?.cancel()
        logger.i("ExtractionVM", "Starting video extraction")
        extractionJob = viewModelScope.launch {
            _uiState.value = ExtractionUiState(
                isLoading = true,
                videoUrl = videoUrl,
                step = ExtractionStep.SCRAPING,
                progressMessage = "Erkenne Plattform...",
                progressPercent = 0.05f
            )

            // Collect progress updates
            val progressJob = launch {
                extractRecipeUseCase.progress.collect { update ->
                    val step = when (update.step) {
                        "detecting", "scraping" -> ExtractionStep.SCRAPING
                        "connecting", "analyzing", "parsing" -> ExtractionStep.EXTRACTING
                        "saving", "done" -> ExtractionStep.EXTRACTING
                        else -> ExtractionStep.EXTRACTING
                    }
                    _uiState.value = _uiState.value.copy(
                        step = step,
                        progressMessage = update.message,
                        progressPercent = update.progress
                    )
                }
            }

            val result = try {
                extractRecipeUseCase(videoUrl)
            } finally {
                progressJob.cancel()
            }

            when (result) {
                is ExtractRecipeUseCase.ExtractionResult.Success -> {
                    logger.i("ExtractionVM", "Extraction succeeded")
                    _uiState.value = ExtractionUiState(
                        isLoading = false,
                        videoUrl = videoUrl,
                        extractedRecipe = result.recipe,
                        step = ExtractionStep.SUCCESS,
                        progressMessage = "Rezept extrahiert!",
                        progressPercent = 1f
                    )
                }
                is ExtractRecipeUseCase.ExtractionResult.NoApiKey -> {
                    logger.w("ExtractionVM", "No API key for: ${result.provider}")
                    _uiState.value = ExtractionUiState(
                        isLoading = false,
                        videoUrl = videoUrl,
                        error = "Bitte konfiguriere deinen ${result.provider} API-Key in den Einstellungen",
                        step = ExtractionStep.ERROR
                    )
                }
                is ExtractRecipeUseCase.ExtractionResult.ScrapingFailed -> {
                    logger.w("ExtractionVM", "Scraping failed, continuing with direct extraction")
                    _uiState.value = ExtractionUiState(
                        isLoading = true,
                        videoUrl = videoUrl,
                        step = ExtractionStep.EXTRACTING,
                        progressMessage = "Video-Scraping fehlgeschlagen, versuche direkte KI-Analyse...",
                        progressPercent = 0.4f
                    )
                    
                    val retryResult = extractRecipeUseCase.invokeDirect(videoUrl)
                    when (retryResult) {
                        is ExtractRecipeUseCase.ExtractionResult.Success -> {
                            _uiState.value = ExtractionUiState(
                                isLoading = false,
                                videoUrl = videoUrl,
                                extractedRecipe = retryResult.recipe,
                                step = ExtractionStep.SUCCESS,
                                progressMessage = "Rezept extrahiert!",
                                progressPercent = 1f
                            )
                        }
                        else -> {
                            _uiState.value = ExtractionUiState(
                                isLoading = false,
                                videoUrl = videoUrl,
                                error = when (retryResult) {
                                    is ExtractRecipeUseCase.ExtractionResult.Error -> retryResult.message
                                    is ExtractRecipeUseCase.ExtractionResult.ScrapingFailed -> retryResult.message
                                    is ExtractRecipeUseCase.ExtractionResult.NoApiKey -> "Bitte konfiguriere deinen ${retryResult.provider} API-Key in den Einstellungen"
                                    else -> "Extraction failed"
                                },
                                step = ExtractionStep.ERROR
                            )
                        }
                    }
                }
                is ExtractRecipeUseCase.ExtractionResult.Error -> {
                    logger.e("ExtractionVM", "Extraction failed")
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
        extractionJob?.cancel()
        extractionJob = null
        _uiState.value = ExtractionUiState()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
