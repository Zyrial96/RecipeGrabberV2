package com.recipegrabber

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.core.app.ApplicationProvider
import com.recipegrabber.data.security.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.UUID

class CredentialKeystoreTest {
    private val alias = "recipegrabber.qa.${UUID.randomUUID()}"
    private val cipher = AndroidKeystoreSecretCipher(alias)
    private val root = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "credential-qa-${UUID.randomUUID()}").also { it.mkdirs() }

    @After fun cleanup() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        root.deleteRecursively()
    }

    @Test fun actualKeystoreRoundTripUsesNonexportableKeyAndRandomIV() {
        val field = CredentialFields.openAi.encrypted.name
        val one = cipher.encrypt("synthetic credential", field)
        val two = cipher.encrypt("synthetic credential", field)
        assertNotEquals(one, two)
        assertEquals("synthetic credential", AndroidKeystoreSecretCipher(alias).decrypt(one, field))
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null)
        assertNull(key.encoded)
    }

    @Test fun missingKeyFailsClosedWithoutCreatingAnotherKey() {
        val field = CredentialFields.gemini.encrypted.name
        val encrypted = cipher.encrypt("synthetic credential", field)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        try { cipher.decrypt(encrypted, field); fail("Missing key must fail") }
        catch (_: GeneralSecurityException) { }
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias))
    }

    @Test fun datastoreMigrationPersistsEncryptedValuesWithoutLegacyFields() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        val legacy = mutablePreferencesOf()
        CredentialFields.all.forEach { legacy[it.legacy] = "synthetic-native-${it.legacyName}" }
        val initialJob = SupervisorJob()
        val initial = PreferenceDataStoreFactory.create(scope = CoroutineScope(initialJob + Dispatchers.IO), produceFile = { file })
        initial.updateData { legacy }
        initialJob.cancelAndJoin()
        val job: Job = SupervisorJob()
        try {
            val migrated = PreferenceDataStoreFactory.create(
                migrations = listOf(CredentialMigration(cipher)),
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file }
            ).data.first()
            assertFalse(credentialsUnavailable(migrated, cipher))
            CredentialFields.all.forEach {
                assertNull(migrated[it.legacy])
                assertEquals(legacy[it.legacy], readCredential(migrated, it, cipher))
            }
            val bytes = file.readBytes().toString(Charsets.UTF_8)
            assertFalse(bytes.contains("synthetic-native-"))
            assertFalse(bytes.contains("openai_api_key\n"))
        } finally { job.cancelAndJoin() }
    }
}
