package ru.kino.dev.settings.data.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import ru.kino.dev.settings.biometric.BiometricFailure
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [BiometricKeys] on the Android Keystore: an AES key that never leaves it and opens only after strong
 * biometrics, for each use - there is no window of validity. Adding or removing a fingerprint invalidates
 * it, so a new finger never reads a password stored before it.
 */
internal class KeystoreBiometricKeys(
    private val context: Context,
) : BiometricKeys {

    override fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    override fun newEncryptCipher(): Cipher {
        deleteKey()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(keySpec())
        return newCipher().apply { init(Cipher.ENCRYPT_MODE, generator.generateKey()) }
    }

    override fun decryptCipher(iv: ByteArray): Cipher {
        val key = keyStore().getKey(KEY_ALIAS, null) as? SecretKey ?: throw BiometricFailure.KeyInvalidated()
        return try {
            newCipher().apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv)) }
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw BiometricFailure.KeyInvalidated(e)
        }
    }

    override fun deleteKey() {
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun keySpec(): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                // Each use, strong biometrics only. Before Android 11 a key that requires authentication
                // with no validity set is already per use and opened by biometrics alone
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()

    private fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        // The name of Step 33, kept when the code moved to `settings`: another one loses the keys of every phone
        const val KEY_ALIAS = "welcome_biometric_password"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val GCM_TAG_BITS = 128
    }
}
