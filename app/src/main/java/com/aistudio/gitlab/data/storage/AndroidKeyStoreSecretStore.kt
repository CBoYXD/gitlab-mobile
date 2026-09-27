package com.aistudio.gitlab.data.storage

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/** Stores short strings encrypted with an AES key that never leaves Android Keystore. */
class AndroidKeyStoreSecretStore(context: Context, name: String) {
  private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
  private val keyAlias = "${context.packageName}.$name.aes"

  @Synchronized
  fun get(key: String): String? {
    val encrypted = preferences.getString(key, null)
    if (encrypted != null) {
      val decoded = runCatching { decrypt(encrypted) }
      decoded.getOrNull()?.let { return it }
      if (looksEncrypted(encrypted)) {
        // Our own ciphertext that failed to open (Keystore hiccup or invalidated key). Never
        // re-save it as plaintext: surface the failure so callers keep the stored value intact.
        throw SecretStoreException("Unable to read saved credentials.", decoded.exceptionOrNull())
      }
      // Older app versions wrote these values in plaintext into this same preferences file.
      put(key, encrypted)
      return encrypted
    }

    // Migrate legacy plaintext values once, then erase the old entry.
    val legacy = preferences.getString("legacy_$key", null)
      ?: preferences.getString(key, null)
      ?: return null
    put(key, legacy)
    preferences.edit().remove("legacy_$key").remove(key).commit()
    return legacy
  }

  @Synchronized
  fun put(key: String, value: String) {
    preferences.edit().putString(key, encrypt(value)).commit()
  }

  @Synchronized
  fun remove(key: String) {
    preferences.edit().remove(key).remove("legacy_$key").commit()
  }

  @Synchronized
  fun clear() {
    preferences.edit().clear().commit()
  }

  private fun looksEncrypted(value: String): Boolean {
    val packed = runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull() ?: return false
    val ivSize = packed.firstOrNull()?.toInt()?.and(0xFF) ?: return false
    // IV length byte + IV + at least the 16-byte GCM tag.
    return ivSize in 12..16 && packed.size >= 1 + ivSize + TAG_BITS / 8
  }

  private fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey())
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val packed = byteArrayOf(iv.size.toByte()) + iv + ciphertext
    return Base64.encodeToString(packed, Base64.NO_WRAP)
  }

  private fun decrypt(value: String): String {
    val packed = Base64.decode(value, Base64.NO_WRAP)
    val ivSize = packed.firstOrNull()?.toInt()?.and(0xFF) ?: error("Invalid encrypted value")
    require(ivSize in 12..16 && packed.size > ivSize + 1) { "Invalid encrypted value" }
    val iv = packed.copyOfRange(1, ivSize + 1)
    val ciphertext = packed.copyOfRange(ivSize + 1, packed.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }

  private fun secretKey(): SecretKey {
    // Robolectric does not provide AndroidKeyStore; keep unit tests isolated from device crypto.
    if (Build.FINGERPRINT.lowercase().contains("robolectric")) {
      return TEST_KEYS.getOrPut(keyAlias) {
        ByteArray(32).also(SecureRandom()::nextBytes).let { SecretKeySpec(it, "AES") }
      }
    }
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
      init(
        KeyGenParameterSpec.Builder(
          keyAlias,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .setRandomizedEncryptionRequired(true)
          .build()
      )
      generateKey()
    }
  }

  private companion object {
    val TEST_KEYS = ConcurrentHashMap<String, SecretKey>()
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val TAG_BITS = 128
  }
}

class SecretStoreException(message: String, cause: Throwable?) : Exception(message, cause)
