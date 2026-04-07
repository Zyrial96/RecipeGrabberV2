package com.recipegrabber.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.LlmModels
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
    // LLM Provider
    val llmProvider: ProviderType = ProviderType.OPENAI,
    val llmModel: String = LlmModels.GPT_4O.id,
    
    // API Keys
    val openAiApiKey: String = "",
    val geminiApiKey: String = "",
    val claudeApiKey: String = "",
    val kimiApiKey: String = "",
    val apifyApiKey: String = "",
    
    // Google Drive
    val driveSyncEnabled: Boolean = false,
    val googleAccountEmail: String = "",
    
    // App Settings
    val clipboardMonitorEnabled: Boolean = true,
    val darkModeEnabled: Boolean = true,
    val autoExtractRecipes: Boolean = true,
    
    // UI State
    val isLoading: Boolean = false,
    val message: String? = null,
    val showApifyHelp: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _showApifyHelp = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.llmProviderType,
        preferencesRepository.llmModel,
        preferencesRepository.openAiApiKey,
        preferencesRepository.geminiApiKey,
        preferencesRepository.claudeApiKey,
        preferencesRepository.kimiApiKey,
        preferencesRepository.apifyApiKey,
        preferencesRepository.driveSyncEnabled,
        preferencesRepository.googleAccountEmail,
        preferencesRepository.clipboardMonitorEnabled,
        preferencesRepository.darkModeEnabled,
        preferencesRepository.autoExtractRecipes,
        _showApifyHelp
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            llmProvider = values[0] as ProviderType,
            llmModel = values[1] as String,
            openAiApiKey = values[2] as String,
            geminiApiKey = values[3] as String,
            claudeApiKey = values[4] as String,
            kimiApiKey = values[5] as String,
            apifyApiKey = values[6] as String,
            driveSyncEnabled = values[7] as Boolean,
            googleAccountEmail = values[8] as String,
            clipboardMonitorEnabled = values[9] as Boolean,
            darkModeEnabled = values[10] as Boolean,
            autoExtractRecipes = values[11] as Boolean,
            showApifyHelp = values[12] as Boolean,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    // LLM Provider
    fun setLlmProvider(type: ProviderType) {
        viewModelScope.launch {
            preferencesRepository.setLlmProvider(type)
            // Set default model for new provider
            val defaultModel = LlmModels.getDefaultModelForProvider(type).id
            preferencesRepository.setLlmModel(defaultModel)
            _message.value = "Provider changed to ${type.name}"
        }
    }

    fun setLlmModel(modelId: String) {
        viewModelScope.launch {
            preferencesRepository.setLlmModel(modelId)
            _message.value = "Model updated"
        }
    }

    // API Keys
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

    fun setClaudeApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setClaudeApiKey(apiKey)
            _message.value = "Claude API key saved"
        }
    }

    fun setKimiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setKimiApiKey(apiKey)
            _message.value = "Kimi API key saved"
        }
    }

    fun setApifyApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setApifyApiKey(apiKey)
            _message.value = "Apify API key saved"
        }
    }

    // Apify Help
    fun showApifyHelp() {
        _showApifyHelp.value = true
    }

    fun dismissApifyHelp() {
        _showApifyHelp.value = false
    }

    // Google Drive
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

    fun signOutGoogle() {
        viewModelScope.launch {
            googleDriveService.signOut()
            preferencesRepository.setDriveSyncEnabled(false)
            preferencesRepository.setGoogleAccountEmail("")
            _message.value = "Signed out from Google"
        }
    }

    // App Settings
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

    fun clearMessage() {
        _message.value = null
    }
}