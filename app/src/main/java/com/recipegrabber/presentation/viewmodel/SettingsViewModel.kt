package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val llmProvider: ProviderType = ProviderType.OPENAI,
    val openAiApiKey: String = "",
    val geminiApiKey: String = "",
    val driveSyncEnabled: Boolean = false,
    val googleAccountEmail: String = "",
    val clipboardMonitorEnabled: Boolean = true,
    val darkModeEnabled: Boolean = true,
    val autoExtractRecipes: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.llmProviderType,
        preferencesRepository.openAiApiKey,
        preferencesRepository.geminiApiKey,
        preferencesRepository.driveSyncEnabled,
        preferencesRepository.googleAccountEmail,
        preferencesRepository.clipboardMonitorEnabled,
        preferencesRepository.darkModeEnabled,
        preferencesRepository.autoExtractRecipes,
        googleDriveService.isSignedIn
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            llmProvider = values[0] as ProviderType,
            openAiApiKey = values[1] as String,
            geminiApiKey = values[2] as String,
            driveSyncEnabled = values[3] as Boolean,
            googleAccountEmail = values[4] as String,
            clipboardMonitorEnabled = values[5] as Boolean,
            darkModeEnabled = values[6] as Boolean,
            autoExtractRecipes = values[7] as Boolean,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    fun setLlmProvider(type: ProviderType) {
        viewModelScope.launch {
            preferencesRepository.setLlmProvider(type)
            _message.value = "Provider changed to ${type.name}"
        }
    }

    fun setOpenAiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setOpenAiApiKey(apiKey)
            _message.value = "OpenAI API key saved"
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setGeminiApiKey(apiKey)
            _message.value = "Gemini API key saved"
        }
    }

    fun setDriveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val result = googleDriveService.signIn()
                result.fold(
                    onSuccess = {
                        preferencesRepository.setDriveSyncEnabled(true)
                        _message.value = "Google Drive sync enabled"
                    },
                    onFailure = {
                        _message.value = "Failed to sign in to Google"
                    }
                )
            } else {
                googleDriveService.signOut()
                preferencesRepository.setDriveSyncEnabled(false)
                _message.value = "Google Drive sync disabled"
            }
        }
    }

    fun setClipboardMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setClipboardMonitorEnabled(enabled)
            _message.value = "Clipboard monitor ${if (enabled) "enabled" else "disabled"}"
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkModeEnabled(enabled)
        }
    }

    fun setAutoExtractRecipes(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoExtractRecipes(enabled)
            _message.value = "Auto-extract ${if (enabled) "enabled" else "disabled"}"
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleDriveService.signOut()
            preferencesRepository.setDriveSyncEnabled(false)
            preferencesRepository.setGoogleAccountEmail("")
            _message.value = "Signed out from Google"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
