package com.recipegrabber.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class AndroidKeystoreSecretCipher(
    private val alias: String = "recipegrabber.credentials.v1"
) : SecretCipher {
    private val cipher = AesGcmSecretCipher(
        encryptionKey = { key(create = true) },
        decryptionKey = { key(create = false) }
    )

    override fun encrypt(plaintext: String, field: String) = secure { cipher.encrypt(plaintext, field) }
    override fun decrypt(envelope: String, field: String) = secure { cipher.decrypt(envelope, field) }

    private fun secure(operation: () -> String): String = try { operation() }
    catch (_: ProviderException) { throw GeneralSecurityException("Credential service is unavailable") }

    private fun key(create: Boolean): SecretKey = synchronized(keyLock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null)
        if (existing is SecretKey) return@synchronized existing
        if (!create || store.containsAlias(alias)) throw GeneralSecurityException("Credential key is unavailable")
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        generator.generateKey()
    }

    private companion object { val keyLock = Any() }
}
