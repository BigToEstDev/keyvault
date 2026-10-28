package ru.kino.dev.ui.biometric

import androidx.compose.runtime.Composable
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
 * Shows the system prompt of strong biometrics, with [Cipher] as the crypto object, so that only a real match
 * unlocks it. One for every screen that asks for biometrics - signing in in `welcome`, turning them on there and in
 * the settings (pass-docs, Step 59, item 7); the cipher is the screen's to keep meanwhile. Launched by the screen
 * when its processor asks with a side effect; the result goes back as an action.
 */
@Composable
expect fun rememberBiometricPrompt(): (Cipher, BiometricPurpose, (BiometricPromptResult) -> Unit) -> Unit
