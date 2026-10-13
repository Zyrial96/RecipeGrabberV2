package com.recipegrabber.data.security

import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecretCipher {
    fun encrypt(plaintext: String, field: String): String
    fun decrypt(envelope: String, field: String): String
}

internal class AesGcmSecretCipher(
    private val encryptionKey: () -> SecretKey,
    private val decryptionKey: () -> SecretKey = encryptionKey
) : SecretCipher {
    override fun encrypt(plaintext: String, field: String): String {
        require(plaintext.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Credential exceeds storage limit" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        cipher.updateAAD(field.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return "1.${encoder.encodeToString(cipher.iv)}.${encoder.encodeToString(encrypted)}"
    }

    override fun decrypt(envelope: String, field: String): String {
        try {
            require(envelope.length <= 24_000)
            val parts = envelope.split('.', limit = 4)
            require(parts.size == 3 && parts[0] == "1")
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[1])
            val encrypted = decoder.decode(parts[2])
            require(iv.size == 12 && encrypted.size in 16..MAX_BYTES + 16)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey(), GCMParameterSpec(128, iv))
            cipher.updateAAD(field.toByteArray(Charsets.UTF_8))
            return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            throw GeneralSecurityException("Saved credential is unavailable")
        } catch (_: IllegalArgumentException) {
            throw GeneralSecurityException("Saved credential is unavailable")
        }
    }

    private companion object { const val MAX_BYTES = 16_384 }
}
