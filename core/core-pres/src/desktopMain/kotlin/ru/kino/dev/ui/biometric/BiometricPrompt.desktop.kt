package ru.kino.dev.ui.biometric

import androidx.compose.runtime.Composable
import javax.crypto.Cipher

// No biometrics on the desktop, not planned yet (pass-docs): they are never offered, so this is never launched
@Composable
actual fun rememberBiometricPrompt(): (Cipher, BiometricPurpose, (BiometricPromptResult) -> Unit) -> Unit =
    { _, _, onResult -> onResult(BiometricPromptResult.Failed) }
