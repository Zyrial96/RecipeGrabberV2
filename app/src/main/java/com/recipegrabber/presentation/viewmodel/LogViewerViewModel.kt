package com.recipegrabber.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogViewerUiState(
    val logContent: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class LogViewerViewModel @Inject constructor(
    private val logger: AppLogger
) : ViewModel() {

    var uiState by mutableStateOf(LogViewerUiState())
        private set

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val content = logger.getLogContent()
                uiState = uiState.copy(logContent = content, isLoading = false)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    logContent = "Diagnostics could not be loaded",
                    isLoading = false
                )
            }
        }
    }

    fun shareLogs(context: Context) {
        viewModelScope.launch {
            try {
                val shareIntent = logger.createShareIntent()
                val chooser = Intent.createChooser(shareIntent, "Share RecipeGrabber Logs")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                uiState = uiState.copy(message = "Diagnostics could not be shared")
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logger.clearLogs()
            uiState = uiState.copy(logContent = "", message = "Logs cleared")
            loadLogs()
        }
    }
}
