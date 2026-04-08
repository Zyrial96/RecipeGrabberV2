package com.recipegrabber.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.LlmModels
import com.recipegrabber.domain.llm.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val llmProvider: ProviderType = ProviderType.OPENAI,
    val llmModel: String = LlmModels.GPT_4O.id,
    val openAiApiKey: String = "",
    val geminiApiKey: String = "",
    val claudeApiKey: String = "",
    val kimiApiKey: String = "",
    val apifyApiKey: String = "",
    val driveSyncEnabled: Boolean = false,
    val googleAccountEmail: String = "",
    val clipboardMonitorEnabled: Boolean = true,
    val darkModeEnabled: Boolean = true,
    val autoExtractRecipes: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null,
    val showApifyHelp: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService,
    private val logger: AppLogger
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _showApifyHelp = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.llmProviderType.collect { _uiState.value = _uiState.value.copy(llmProvider = it) }
        }
        viewModelScope.launch {
            preferencesRepository.llmModel.collect { _uiState.value = _uiState.value.copy(llmModel = it) }
        }
        viewModelScope.launch {
            preferencesRepository.openAiApiKey.collect { _uiState.value = _uiState.value.copy(openAiApiKey = it) }
        }
        viewModelScope.launch {
            preferencesRepository.geminiApiKey.collect { _uiState.value = _uiState.value.copy(geminiApiKey = it) }
        }
        viewModelScope.launch {
            preferencesRepository.claudeApiKey.collect { _uiState.value = _uiState.value.copy(claudeApiKey = it) }
        }
        viewModelScope.launch {
            preferencesRepository.kimiApiKey.collect { _uiState.value = _uiState.value.copy(kimiApiKey = it) }
        }
        viewModelScope.launch {
            preferencesRepository.apifyApiKey.collect { _uiState.value = _uiState.value.copy(apifyApiKey = it) }
        }
        viewModelScope.launch {
            preferencesRepository.driveSyncEnabled.collect { _uiState.value = _uiState.value.copy(driveSyncEnabled = it) }
        }
        viewModelScope.launch {
            preferencesRepository.googleAccountEmail.collect { _uiState.value = _uiState.value.copy(googleAccountEmail = it) }
        }
        viewModelScope.launch {
            preferencesRepository.clipboardMonitorEnabled.collect { _uiState.value = _uiState.value.copy(clipboardMonitorEnabled = it) }
        }
        viewModelScope.launch {
            preferencesRepository.darkModeEnabled.collect { _uiState.value = _uiState.value.copy(darkModeEnabled = it) }
        }
        viewModelScope.launch {
            preferencesRepository.autoExtractRecipes.collect { _uiState.value = _uiState.value.copy(autoExtractRecipes = it) }
        }
        viewModelScope.launch {
            _showApifyHelp.collect { _uiState.value = _uiState.value.copy(showApifyHelp = it) }
        }
        viewModelScope.launch {
            _message.collect { _uiState.value = _uiState.value.copy(message = it) }
        }
    }

    fun setLlmProvider(type: ProviderType) {
        viewModelScope.launch {
            preferencesRepository.setLlmProvider(type)
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

    fun showApifyHelp() {
        _showApifyHelp.value = true
    }

    fun dismissApifyHelp() {
        _showApifyHelp.value = false
    }

    fun getSignInIntent(): Intent {
        return googleDriveService.getSignInIntent()
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _message.value = null
            try {
                val result = googleDriveService.handleSignInResult(data)
                result.fold(
                    onSuccess = { email ->
                        preferencesRepository.setGoogleAccountEmail(email)
                        preferencesRepository.setDriveSyncEnabled(true)
                        _message.value = "Google Drive sync enabled"
                    },
                    onFailure = {
                        _message.value = "Failed to sign in to Google"
                    }
                )
            } catch (e: Exception) {
                _message.value = "Failed to sign in to Google"
            }
        }
    }

    fun setDriveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                _message.value = "Please sign in using the Google Drive settings"
            } else {
                googleDriveService.signOut()
                preferencesRepository.setDriveSyncEnabled(false)
                preferencesRepository.setGoogleAccountEmail("")
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