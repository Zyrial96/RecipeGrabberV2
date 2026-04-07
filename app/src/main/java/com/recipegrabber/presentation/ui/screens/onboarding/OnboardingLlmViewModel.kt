package com.recipegrabber.presentation.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.LlmModels
import com.recipegrabber.domain.llm.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class OnboardingLlmViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _selectedProvider = MutableStateFlow(ProviderType.OPENAI)
    val selectedProvider: StateFlow<ProviderType> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _showKey = MutableStateFlow(false)
    val showKey: StateFlow<Boolean> = _showKey.asStateFlow()

    init {
        viewModelScope.launch {
            // Load saved provider
            preferencesRepository.llmProviderType.collect { provider ->
                _selectedProvider.value = provider
                // Set default model for provider if none selected
                if (_selectedModel.value == null) {
                    _selectedModel.value = LlmModels.getDefaultModelForProvider(provider).id
                }
            }
        }

        viewModelScope.launch {
            // Load saved model
            preferencesRepository.llmModel.collect { model ->
                if (model.isNotEmpty()) {
                    _selectedModel.value = model
                }
            }
        }

        viewModelScope.launch {
            // Load API key based on provider
            preferencesRepository.llmProviderType.collect { provider ->
                val keyFlow = when (provider) {
                    ProviderType.OPENAI -> preferencesRepository.openAiApiKey
                    ProviderType.GEMINI -> preferencesRepository.geminiApiKey
                    ProviderType.CLAUDE -> preferencesRepository.claudeApiKey
                    ProviderType.KIMI -> preferencesRepository.kimiApiKey
                }
                keyFlow.collect { key ->
                    _apiKey.value = key
                    validateKey(key)
                }
            }
        }

        // Auto-save API key
        viewModelScope.launch {
            _apiKey.debounce(500).collect { key ->
                if (key.length >= 10) {
                    saveApiKey(key)
                }
            }
        }
    }

    fun onProviderSelected(provider: ProviderType) {
        _selectedProvider.value = provider
        _selectedModel.value = LlmModels.getDefaultModelForProvider(provider).id
        _apiKey.value = "" // Reset key when switching providers
        _isValid.value = false
        viewModelScope.launch {
            preferencesRepository.setLlmProvider(provider)
        }
    }

    fun onModelSelected(modelId: String) {
        _selectedModel.value = modelId
        viewModelScope.launch {
            preferencesRepository.setLlmModel(modelId)
        }
    }

    fun onApiKeyChange(key: String) {
        _apiKey.value = key
        validateKey(key)
    }

    fun toggleShowKey() {
        _showKey.value = !_showKey.value
    }

    private fun validateKey(key: String) {
        _isValid.value = when (_selectedProvider.value) {
            ProviderType.OPENAI -> key.startsWith("sk-") && key.length > 20
            ProviderType.GEMINI -> key.length > 20
            ProviderType.CLAUDE -> key.startsWith("sk-ant-") && key.length > 20
            ProviderType.KIMI -> key.length > 20
        }
    }

    private suspend fun saveApiKey(key: String) {
        when (_selectedProvider.value) {
            ProviderType.OPENAI -> preferencesRepository.setOpenAiApiKey(key)
            ProviderType.GEMINI -> preferencesRepository.setGeminiApiKey(key)
            ProviderType.CLAUDE -> preferencesRepository.setClaudeApiKey(key)
            ProviderType.KIMI -> preferencesRepository.setKimiApiKey(key)
        }
    }
}