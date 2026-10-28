package ru.kino.dev.ui.biometric

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource
import javax.crypto.Cipher

// The system `BiometricPrompt` - strong biometrics only, the cipher as the crypto object
@Composable
actual fun rememberBiometricPrompt(): (Cipher, BiometricPurpose, (BiometricPromptResult) -> Unit) -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    val texts = PromptTexts(
        unlockTitle = stringResource(Res.string.biometric_prompt_unlock_title),
        enableTitle = stringResource(Res.string.biometric_prompt_enable_title),
        usePassword = stringResource(Res.string.biometric_prompt_use_password),
        cancel = stringResource(Res.string.action_cancel),
    )

    return remember(activity, texts) {
        { cipher, purpose, onResult ->
            activity?.let { host ->
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onResult(BiometricPromptResult.Succeeded)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onResult(
                            if (errorCode in CANCELLED) BiometricPromptResult.Cancelled else BiometricPromptResult.Failed,
                        )
                    }
                    // A finger that does not match is retried by the prompt itself
                }
                BiometricPrompt(host, ContextCompat.getMainExecutor(host), callback)
                    .authenticate(texts.info(purpose), BiometricPrompt.CryptoObject(cipher))
            }
        }
    }
}

private data class PromptTexts(
    val unlockTitle: String,
    val enableTitle: String,
    val usePassword: String,
    val cancel: String,
) {
    fun info(purpose: BiometricPurpose): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (purpose == BiometricPurpose.Unlock) unlockTitle else enableTitle)
            .setNegativeButtonText(if (purpose == BiometricPurpose.Unlock) usePassword else cancel)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
}

/** Errors that are the user's choice, not a failure. */
private val CANCELLED = setOf(
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_CANCELED,
)
