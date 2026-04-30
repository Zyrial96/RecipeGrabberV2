package com.recipegrabber.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recipegrabber.domain.llm.GeminiAuthMode
import com.recipegrabber.domain.llm.ProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        
        // LLM Provider
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val GEMINI_AUTH_MODE = stringPreferencesKey("gemini_auth_mode")
        val GEMINI_OAUTH_AUTHORIZED = booleanPreferencesKey("gemini_oauth_authorized")
        
        // API Keys
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val KIMI_API_KEY = stringPreferencesKey("kimi_api_key")
        val APIFY_API_KEY = stringPreferencesKey("apify_api_key")
        
        // Google Drive
        val DRIVE_SYNC_ENABLED = booleanPreferencesKey("drive_sync_enabled")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        
        // App Settings
        val CLIPBOARD_MONITOR_ENABLED = booleanPreferencesKey("clipboard_monitor_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val AUTO_EXTRACT_RECIPES = booleanPreferencesKey("auto_extract_recipes")
    }
    
    // Onboarding Status
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    // LLM Model
    val llmModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LLM_MODEL] ?: ""
    }
    
    suspend fun setLlmModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_MODEL] = model
        }
    }
    
    // Claude
    val claudeApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CLAUDE_API_KEY] ?: ""
    }
    
    suspend fun setClaudeApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLAUDE_API_KEY] = apiKey
        }
    }
    
    // Kimi
    val kimiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KIMI_API_KEY] ?: ""
    }
    
    suspend fun setKimiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KIMI_API_KEY] = apiKey
        }
    }
    
    // Apify
    val apifyApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APIFY_API_KEY] ?: ""
    }
    
    suspend fun setApifyApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APIFY_API_KEY] = apiKey
        }
    }

    val llmProviderType: Flow<ProviderType> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.LLM_PROVIDER] ?: ProviderType.OPENAI.name
        try {
            ProviderType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ProviderType.OPENAI
        }
    }

    val geminiAuthMode: Flow<GeminiAuthMode> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.GEMINI_AUTH_MODE] ?: GeminiAuthMode.API_KEY.name
        try {
            GeminiAuthMode.valueOf(value)
        } catch (e: IllegalArgumentException) {
            GeminiAuthMode.API_KEY
        }
    }

    val geminiOAuthAuthorized: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_OAUTH_AUTHORIZED] ?: false
    }

    val openAiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OPENAI_API_KEY] ?: ""
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
    }

    val driveSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DRIVE_SYNC_ENABLED] ?: false
    }

    val googleAccountEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_ACCOUNT_EMAIL] ?: ""
    }

    val clipboardMonitorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CLIPBOARD_MONITOR_ENABLED] ?: true
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE_ENABLED] ?: true
    }

    val autoExtractRecipes: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_EXTRACT_RECIPES] ?: true
    }

    suspend fun setLlmProvider(type: ProviderType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_PROVIDER] = type.name
        }
    }

    suspend fun setGeminiAuthMode(mode: GeminiAuthMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_AUTH_MODE] = mode.name
        }
    }

    suspend fun setGeminiOAuthAuthorized(authorized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_OAUTH_AUTHORIZED] = authorized
        }
    }

    suspend fun setOpenAiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_API_KEY] = apiKey
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun setDriveSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVE_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setGoogleAccountEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_ACCOUNT_EMAIL] = email
        }
    }

    suspend fun setClipboardMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLIPBOARD_MONITOR_ENABLED] = enabled
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun setAutoExtractRecipes(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_EXTRACT_RECIPES] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
