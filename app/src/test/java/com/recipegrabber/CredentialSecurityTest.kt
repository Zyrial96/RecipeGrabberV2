package com.recipegrabber

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.recipegrabber.data.security.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException
import javax.crypto.spec.SecretKeySpec

class CredentialSecurityTest {
    private val cipher = AesGcmSecretCipher({ SecretKeySpec(ByteArray(32) { 7 }, "AES") })
    private val field = CredentialFields.openAi

    @Test fun `randomized authenticated encryption preserves exact UTF8 value`() {
        val value = "synthetic credential \u00e4\n with whitespace "
        val one = cipher.encrypt(value, field.encrypted.name)
        val two = cipher.encrypt(value, field.encrypted.name)
        assertNotEquals(one, two)
        assertFalse(one.contains(value))
        assertEquals(value, cipher.decrypt(one, field.encrypted.name))
    }

    @Test fun `wrong field and wrong key cannot decrypt ciphertext`() {
        val encrypted = cipher.encrypt("synthetic credential", field.encrypted.name)
        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(encrypted, CredentialFields.gemini.encrypted.name) }
        val other = AesGcmSecretCipher({ SecretKeySpec(ByteArray(32) { 8 }, "AES") })
        assertThrows(GeneralSecurityException::class.java) { other.decrypt(encrypted, field.encrypted.name) }
    }

    @Test fun `tampering unknown versions and malformed envelopes fail closed`() {
        val encrypted = cipher.encrypt("synthetic credential", field.encrypted.name)
        val parts = encrypted.split('.').toMutableList()
        val bytes = java.util.Base64.getDecoder().decode(parts[2])
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        parts[2] = java.util.Base64.getEncoder().encodeToString(bytes)
        for (invalid in listOf(parts.joinToString("."), "2" + encrypted.drop(1), "1.bad.bad", "plain", "1...", "x".repeat(24001))) {
            assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(invalid, field.encrypted.name) }
        }
    }

    @Test fun `migration encrypts all five keys preserves settings and is idempotent`() = runTest {
        val enabled = booleanPreferencesKey("onboarding_completed")
        val original = mutablePreferencesOf(enabled to true)
        CredentialFields.all.forEach { original[it.legacy] = "synthetic-${it.legacyName}" }
        val migration = CredentialMigration(cipher)
        assertTrue(migration.shouldMigrate(original))
        val migrated = migration.migrate(original)
        assertTrue(migrated[enabled] == true)
        assertFalse(migration.shouldMigrate(migrated))
        for (key in CredentialFields.all) {
            assertNull(migrated[key.legacy])
            assertEquals(original[key.legacy], readCredential(migrated, key, cipher))
        }
        assertFalse(credentialsUnavailable(migrated, cipher))
        assertEquals(migrated, migration.migrate(migrated))
        assertNotNull(original[field.legacy])
    }

    @Test fun `a mid migration failure retains every original value and readers never use plaintext`() = runTest {
        val original = mutablePreferencesOf(field.legacy to "synthetic openai", CredentialFields.gemini.legacy to "synthetic gemini")
        val failing = object : SecretCipher {
            override fun encrypt(plaintext: String, field: String): String {
                if (field.contains("gemini")) throw GeneralSecurityException("Synthetic failure")
                return "synthetic encrypted value"
            }
            override fun decrypt(envelope: String, field: String) = throw GeneralSecurityException("Unavailable")
        }
        val migrated = CredentialMigration(failing).migrate(original)
        assertEquals(original, migrated)
        assertNull(migrated[field.encrypted])
        assertEquals("", readCredential(migrated, field, failing))
        assertTrue(credentialsUnavailable(migrated, failing))
    }

    @Test fun `empty legacy keys are removed and invalid stored ciphertext reports unavailable`() = runTest {
        val empty = CredentialMigration(cipher).migrate(mutablePreferencesOf(field.legacy to ""))
        assertNull(empty[field.legacy])
        assertNull(empty[field.encrypted])
        assertFalse(credentialsUnavailable(empty, cipher))
        val corrupt = mutablePreferencesOf(field.encrypted to "synthetic corrupt ciphertext")
        assertEquals("", readCredential(corrupt, field, cipher))
        assertTrue(credentialsUnavailable(corrupt, cipher))
    }
}
