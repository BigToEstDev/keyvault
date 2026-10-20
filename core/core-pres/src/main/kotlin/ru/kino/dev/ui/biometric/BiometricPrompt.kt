package ru.kino.dev.ui.biometric

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.kino.dev.core.pres.R
import javax.crypto.Cipher

/** How the system prompt ended. */
sealed interface BiometricPromptResult {

    /** The biometrics matched, and the cipher handed to the prompt is unlocked for one use. */
    data object Succeeded : BiometricPromptResult

    /** The user chose the password, or put the prompt away - nothing to say. */
    data object Cancelled : BiometricPromptResult

    /** The system refused - too many attempts, the sensor busy; the password still works. */
    data object Failed : BiometricPromptResult
}

/** What the prompt is shown for - its texts. */
enum class BiometricPurpose {
    Unlock,
    Enable,
}

/**
 * Shows the system `BiometricPrompt` - strong biometrics only, with [Cipher] as the crypto object, so that only
 * a real match unlocks it. One for every screen that asks for biometrics - signing in in `welcome`, turning them
 * on there and in the settings (pass-docs, Step 59, item 7); the cipher is the screen's to keep meanwhile.
 * Launched by the screen when its processor asks with a side effect; the result goes back as an action.
 */
@Composable
fun rememberBiometricPrompt(): (Cipher, BiometricPurpose, (BiometricPromptResult) -> Unit) -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    val texts = PromptTexts(
        unlockTitle = stringResource(R.string.biometric_prompt_unlock_title),
        enableTitle = stringResource(R.string.biometric_prompt_enable_title),
        usePassword = stringResource(R.string.biometric_prompt_use_password),
        cancel = stringResource(R.string.action_cancel),
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
