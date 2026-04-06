package com.recipegrabber.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val DRIVE_SYNC_ENABLED = booleanPreferencesKey("drive_sync_enabled")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val CLIPBOARD_MONITOR_ENABLED = booleanPreferencesKey("clipboard_monitor_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val AUTO_EXTRACT_RECIPES = booleanPreferencesKey("auto_extract_recipes")
    }

    val llmProviderType: Flow<ProviderType> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.LLM_PROVIDER] ?: ProviderType.OPENAI.name
        try {
            ProviderType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ProviderType.OPENAI
        }
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
