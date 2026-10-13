package com.recipegrabber.presentation.viewmodel

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.remote.GoogleAiAuthorizationStart
import com.recipegrabber.data.remote.GoogleDriveService
import com.recipegrabber.data.remote.DriveBackupAuthorization
import com.recipegrabber.data.remote.DriveArchive
import com.recipegrabber.domain.backup.DriveBackupCoordinator
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.domain.llm.GeminiAuthMode
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
    val driveBackupConnected: Boolean = false,
    val driveBackupAccount: String = "",
    val driveArchives: List<DriveArchive> = emptyList(),
    val driveBusy: Boolean = false,
    val driveStatus: String? = null,
    val llmProvider: ProviderType = ProviderType.OPENAI,
    val llmModel: String = LlmModels.GPT_4O.id,
    val geminiAuthMode: GeminiAuthMode = GeminiAuthMode.API_KEY,
    val geminiOAuthAuthorized: Boolean = false,
    val credentialsUnavailable: Boolean = false,
    val openAiApiKey: String = "",
    val geminiApiKey: String = "",
    val claudeApiKey: String = "",
    val kimiApiKey: String = "",
    val apifyApiKey: String = "",
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
    private val driveAuthorization: DriveBackupAuthorization,
    private val driveBackups: DriveBackupCoordinator,
    private val logger: AppLogger
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _showApifyHelp = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    private val credentialSaveJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var driveJob: kotlinx.coroutines.Job? = null

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.driveBackupConnected.collect { _uiState.value = _uiState.value.copy(driveBackupConnected = it) }
        }
        viewModelScope.launch {
            preferencesRepository.driveBackupAccount.collect { _uiState.value = _uiState.value.copy(driveBackupAccount = it) }
        }
        viewModelScope.launch {
            preferencesRepository.llmProviderType.collect { _uiState.value = _uiState.value.copy(llmProvider = it) }
        }
        viewModelScope.launch {
            preferencesRepository.llmModel.collect { _uiState.value = _uiState.value.copy(llmModel = it) }
        }
        viewModelScope.launch {
            preferencesRepository.geminiAuthMode.collect { _uiState.value = _uiState.value.copy(geminiAuthMode = it) }
        }
        viewModelScope.launch {
            preferencesRepository.geminiOAuthAuthorized.collect { _uiState.value = _uiState.value.copy(geminiOAuthAuthorized = it) }
        }
        viewModelScope.launch {
            preferencesRepository.credentialsUnavailable.collect {
                _uiState.value = _uiState.value.copy(credentialsUnavailable = it)
            }
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

    fun setGeminiAuthMode(mode: GeminiAuthMode) {
        viewModelScope.launch {
            preferencesRepository.setGeminiAuthMode(mode)
            _message.value = when (mode) {
                GeminiAuthMode.API_KEY -> "Gemini uses API key authentication"
                GeminiAuthMode.GOOGLE_OAUTH -> {
                    if (_uiState.value.geminiOAuthAuthorized) {
                        "Gemini uses Google OAuth"
                    } else {
                        "Connect Google OAuth to finish setup"
                    }
                }
            }
        }
    }

    fun setOpenAiApiKey(apiKey: String) {
        saveCredential("OpenAI") {
            preferencesRepository.setOpenAiApiKey(apiKey)
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        saveCredential("Gemini") {
            preferencesRepository.setGeminiApiKey(apiKey)
        }
    }

    fun setClaudeApiKey(apiKey: String) {
        saveCredential("Claude") {
            preferencesRepository.setClaudeApiKey(apiKey)
        }
    }

    fun setKimiApiKey(apiKey: String) {
        saveCredential("Kimi") {
            preferencesRepository.setKimiApiKey(apiKey)
        }
    }

    fun setApifyApiKey(apiKey: String) {
        saveCredential("Apify") {
            preferencesRepository.setApifyApiKey(apiKey)
        }
    }

    private fun saveCredential(provider: String, save: suspend () -> Unit) {
        credentialSaveJobs.remove(provider)?.cancel()
        credentialSaveJobs[provider] = viewModelScope.launch {
            try {
                save()
                _message.value = "$provider API key saved"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _message.value = "Could not securely save $provider API key"
            }
        }
    }

    fun showApifyHelp() {
        _showApifyHelp.value = true
    }

    fun dismissApifyHelp() {
        _showApifyHelp.value = false
    }

    fun startGoogleAiAuthorization(includeFiles: Boolean = false, onUserConsentRequired: (PendingIntent) -> Unit) {
        viewModelScope.launch {
            _message.value = null
            val result = googleDriveService.authorizeGenerativeLanguage(includeFiles)
            result.fold(
                onSuccess = { start ->
                    when (start) {
                        is GoogleAiAuthorizationStart.NeedsUserConsent -> {
                            onUserConsentRequired(start.pendingIntent)
                        }
                        is GoogleAiAuthorizationStart.Authorized -> {
                            start.email?.let { preferencesRepository.setGoogleAccountEmail(it) }
                            preferencesRepository.setGeminiOAuthAuthorized(true)
                            preferencesRepository.setGeminiAuthMode(GeminiAuthMode.GOOGLE_OAUTH)
                            _message.value = if (includeFiles) "Google video uploads authorized" else "Google OAuth enabled for Gemini"
                        }
                    }
                },
                onFailure = { error ->
                    logger.e("GoogleOAuth", "Failed to start Google AI authorization", error)
                    _message.value = "Failed to authorize Google OAuth: ${error.message ?: "unknown error"}"
                }
            )
        }
    }

    fun handleGoogleAiAuthorizationResult(resultCode: Int, data: Intent?, includeFiles: Boolean = false) {
        viewModelScope.launch {
            _message.value = null
            logger.i("GoogleOAuth", "Authorization activity finished with resultCode=$resultCode, hasData=${data != null}")
            if (resultCode != android.app.Activity.RESULT_OK || data == null) {
                logger.w("GoogleOAuth", "Authorization activity did not return result data")
                _message.value = "Google OAuth cancelled"
                return@launch
            }

            val result = googleDriveService.handleGenerativeLanguageAuthorizationResult(data, includeFiles)
            result.fold(
                onSuccess = { authorization ->
                    authorization.email?.let { preferencesRepository.setGoogleAccountEmail(it) }
                    preferencesRepository.setGeminiOAuthAuthorized(true)
                    preferencesRepository.setGeminiAuthMode(GeminiAuthMode.GOOGLE_OAUTH)
                    _message.value = if (includeFiles) "Google video uploads authorized" else "Google OAuth enabled for Gemini"
                },
                onFailure = { error ->
                    logger.e("GoogleOAuth", "Failed to finish Google AI authorization", error)
                    _message.value = "Failed to authorize Google OAuth: ${error.message ?: "unknown error"}"
                }
            )
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

    private fun driveOperation(action: suspend () -> Unit) {
        if (driveJob?.isActive == true) return
        driveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(driveBusy = true, driveStatus = null)
            try {
                action()
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(driveStatus = "Operation cancelled. Refresh backups before retrying.")
                throw e
            } catch (e: Exception) {
                logger.e("DriveBackup", "Drive operation failed", e)
                _uiState.value = _uiState.value.copy(driveStatus = "Drive operation failed. Check your connection and reconnect Google Drive if permission expired.")
            } finally {
                _uiState.value = _uiState.value.copy(driveBusy = false)
            }
        }
    }

    fun connectDrive(onConsent: (PendingIntent) -> Unit) = driveOperation {
        when (val start = driveAuthorization.start()) {
            is GoogleAiAuthorizationStart.NeedsUserConsent -> onConsent(start.pendingIntent)
            is GoogleAiAuthorizationStart.Authorized -> _uiState.value = _uiState.value.copy(driveArchives = driveBackups.list())
        }
    }

    fun finishDriveAuthorization(resultCode: Int, data: Intent?) = driveOperation {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            _uiState.value = _uiState.value.copy(driveStatus = "Google Drive connection cancelled")
        } else {
            driveAuthorization.finish(data)
            _uiState.value = _uiState.value.copy(driveArchives = driveBackups.list(), driveStatus = "Google Drive connected")
        }
    }

    fun refreshDriveBackups() = driveOperation {
        _uiState.value = _uiState.value.copy(driveArchives = driveBackups.list(), driveStatus = "Backups refreshed")
    }

    fun backupToDrive() = driveOperation {
        val count = driveBackups.backup()
        _uiState.value = _uiState.value.copy(driveStatus = "Backup saved: $count recipes")
        _uiState.value = _uiState.value.copy(driveArchives = driveBackups.list())
    }

    fun restoreDriveBackup(archive: DriveArchive) = driveOperation {
        check(_uiState.value.driveArchives.any { it.id == archive.id })
        val result = driveBackups.restore(archive.id)
        _uiState.value = _uiState.value.copy(driveStatus = "Restored ${result.inserted} recipes; ${result.skipped} existing recipes kept")
    }

    fun disconnectDrive() = driveOperation {
        preferencesRepository.setDriveBackupConnection(false)
        _uiState.value = _uiState.value.copy(driveArchives = emptyList(), driveStatus = "Drive disconnected; cloud backups retained")
    }

    fun cancelDriveOperation() { driveJob?.cancel() }
}
