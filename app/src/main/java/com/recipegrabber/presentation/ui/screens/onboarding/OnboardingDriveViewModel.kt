package com.recipegrabber.presentation.ui.screens.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingDriveViewModel @Inject constructor(
    private val googleDriveService: GoogleDriveService,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.googleAccountEmail.collect { email ->
                _isConnected.value = email.isNotEmpty()
                _userEmail.value = email.ifEmpty { null }
            }
        }
    }

    fun getSignInIntent(): Intent {
        return googleDriveService.getSignInIntent()
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = googleDriveService.handleSignInResult(data)
                result.onSuccess { email ->
                    preferencesRepository.setGoogleAccountEmail(email)
                    preferencesRepository.setDriveSyncEnabled(true)
                    _isConnected.value = true
                    _userEmail.value = email
                }.onFailure { e ->
                    _error.value = e.message ?: "Anmeldung fehlgeschlagen"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unbekannter Fehler"
            } finally {
                _isLoading.value = false
            }
        }
    }
}