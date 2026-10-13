package com.recipegrabber.presentation.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.repository.PreferencesRepository
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
class OnboardingApifyViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _storageError = MutableStateFlow<String?>(null)
    val storageError: StateFlow<String?> = _storageError.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.apifyApiKey.collect { key ->
                _apiKey.value = key
                _isValid.value = key.length >= 20 // Basic validation
            }
        }

        // Debounced save
        viewModelScope.launch {
            _apiKey.debounce(500).collect { key ->
                if (key.length >= 20) {
                    try {
                        preferencesRepository.setApifyApiKey(key)
                        _storageError.value = null
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        _storageError.value = "API-Token konnte nicht sicher gespeichert werden."
                    }
                }
            }
        }
    }

    fun onApiKeyChange(newKey: String) {
        _apiKey.value = newKey
        _isValid.value = newKey.length >= 20
    }
}
