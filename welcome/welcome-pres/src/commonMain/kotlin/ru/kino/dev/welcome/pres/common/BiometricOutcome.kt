package ru.kino.dev.welcome.pres.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import ru.kino.dev.settings.biometric.BiometricSession
import ru.kino.dev.ui.biometric.BiometricPromptResult
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.biometric.rememberBiometricPrompt as rememberSystemPrompt

/** How the system prompt ended, for the processors of `welcome` - with the session it was shown for. */
sealed interface BiometricOutcome {

    /** The biometrics matched, and the session's cipher is unlocked for one use. */
    data class Succeeded(val session: BiometricSession) : BiometricOutcome

    /** The user chose the password, or put the prompt away - nothing to say. */
    data object Cancelled : BiometricOutcome

    /** The system refused - too many attempts, the sensor busy; the password still works. */
    data object Failed : BiometricOutcome
}

/**
 * The prompt of `core-pres` for a session handed out by the repository: the session goes back with the outcome,
 * so the processor carries on with it (pass-docs, Step 33; the prompt itself - Step 59, item 7).
 */
@Composable
internal fun rememberBiometricPrompt(onOutcome: (BiometricOutcome) -> Unit): (BiometricSession, BiometricPurpose) -> Unit {
    val prompt = rememberSystemPrompt()
    val outcome by rememberUpdatedState(onOutcome)

    return remember(prompt) {
        { session, purpose ->
            prompt(session.cipher, purpose) { result ->
                outcome(
                    when (result) {
                        BiometricPromptResult.Succeeded -> BiometricOutcome.Succeeded(session)
                        BiometricPromptResult.Cancelled -> BiometricOutcome.Cancelled
                        BiometricPromptResult.Failed -> BiometricOutcome.Failed
                    },
                )
            }
        }
    }
}
