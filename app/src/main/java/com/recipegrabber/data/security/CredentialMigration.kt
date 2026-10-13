package com.recipegrabber.data.security

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.security.GeneralSecurityException

internal data class CredentialField(val legacyName: String) {
    val legacy = stringPreferencesKey(legacyName)
    val encrypted = stringPreferencesKey("${legacyName}_encrypted_v1")
}

internal object CredentialFields {
    val openAi = CredentialField("openai_api_key")
    val gemini = CredentialField("gemini_api_key")
    val claude = CredentialField("claude_api_key")
    val kimi = CredentialField("kimi_api_key")
    val apify = CredentialField("apify_api_key")
    val all = listOf(openAi, gemini, claude, kimi, apify)
}

internal class CredentialMigration(private val cipher: SecretCipher) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) = CredentialFields.all.any { currentData[it.legacy] != null }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val result = currentData.toMutablePreferences()
        try {
            for (field in CredentialFields.all) {
                val plaintext = currentData[field.legacy] ?: continue
                if (plaintext.isEmpty()) result.remove(field.encrypted)
                else result[field.encrypted] = cipher.encrypt(plaintext, field.encrypted.name)
                result.remove(field.legacy)
            }
            return result
        } catch (_: GeneralSecurityException) {
            // Preserve the entire original transaction; readers never use legacy plaintext.
            return currentData
        } catch (_: IllegalArgumentException) {
            return currentData
        }
    }

    override suspend fun cleanUp() = Unit
}

internal fun readCredential(preferences: Preferences, field: CredentialField, cipher: SecretCipher): String {
    val encrypted = preferences[field.encrypted] ?: return ""
    return try { cipher.decrypt(encrypted, field.encrypted.name) }
    catch (_: GeneralSecurityException) { "" }
}

internal fun credentialsUnavailable(preferences: Preferences, cipher: SecretCipher): Boolean = CredentialFields.all.any { field ->
    if (preferences[field.legacy] != null) true
    else preferences[field.encrypted]?.let {
        try { cipher.decrypt(it, field.encrypted.name); false }
        catch (_: GeneralSecurityException) { true }
    } ?: false
}
