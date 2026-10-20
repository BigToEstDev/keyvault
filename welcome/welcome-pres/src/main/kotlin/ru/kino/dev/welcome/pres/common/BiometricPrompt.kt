package ru.kino.dev.welcome.pres.common

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.biometric.BiometricSession

/** How the system prompt ended. */
sealed interface BiometricOutcome {

    /** The biometrics matched, and the session's cipher is unlocked for one use. */
    data class Succeeded(val session: BiometricSession) : BiometricOutcome

    /** The user chose the password, or put the prompt away - nothing to say. */
    data object Cancelled : BiometricOutcome

    /** The system refused - too many attempts, the sensor busy; the password still works. */
    data object Failed : BiometricOutcome
}

/** What the prompt is shown for - its texts. */
enum class BiometricPurpose {
    Unlock,
    Enable,
}

/**
 * Shows the system `BiometricPrompt` for a session handed out by the repository - strong biometrics only,
 * with the session's cipher as the crypto object, so that only a real match unlocks it. Launched by the
 * screen when its processor asks with a side effect; the outcome goes back as an action.
 */
@Composable
internal fun rememberBiometricPrompt(onOutcome: (BiometricOutcome) -> Unit): (BiometricSession, BiometricPurpose) -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    val outcome by rememberUpdatedState(onOutcome)
    val texts = PromptTexts(
        unlockTitle = stringResource(R.string.biometric_prompt_unlock_title),
        enableTitle = stringResource(R.string.biometric_prompt_enable_title),
        usePassword = stringResource(R.string.biometric_prompt_use_password),
        cancel = stringResource(R.string.action_cancel),
    )

    return remember(activity, texts) {
        { session, purpose ->
            activity?.let { host ->
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        outcome(BiometricOutcome.Succeeded(session))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        outcome(if (errorCode in CANCELLED) BiometricOutcome.Cancelled else BiometricOutcome.Failed)
                    }
                    // A finger that does not match is retried by the prompt itself
                }
                BiometricPrompt(host, ContextCompat.getMainExecutor(host), callback)
                    .authenticate(texts.info(purpose), BiometricPrompt.CryptoObject(session.cipher))
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
