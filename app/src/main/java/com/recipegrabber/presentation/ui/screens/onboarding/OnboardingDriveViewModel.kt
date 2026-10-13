package com.recipegrabber.presentation.ui.screens.onboarding

import android.content.Intent
import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.remote.DriveBackupAuthorization
import com.recipegrabber.data.remote.GoogleAiAuthorizationStart
import com.recipegrabber.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingDriveViewModel @Inject constructor(
    private val authorization: DriveBackupAuthorization,
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
            preferencesRepository.driveBackupAccount.collect { email ->
                _userEmail.value = email.ifEmpty { null }
            }
        }
        viewModelScope.launch { preferencesRepository.driveBackupConnected.collect { _isConnected.value = it } }
    }

    private fun operation(action: suspend () -> Unit) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                action()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Google Drive connection failed. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun connect(onConsent: (PendingIntent) -> Unit) = operation {
        when (val start = authorization.start()) {
            is GoogleAiAuthorizationStart.NeedsUserConsent -> onConsent(start.pendingIntent)
            is GoogleAiAuthorizationStart.Authorized -> Unit
        }
    }

    fun finish(resultCode: Int, data: Intent?) = operation {
        if (resultCode == android.app.Activity.RESULT_OK && data != null) authorization.finish(data)
        else _error.value = "Google Drive connection cancelled"
    }
}
