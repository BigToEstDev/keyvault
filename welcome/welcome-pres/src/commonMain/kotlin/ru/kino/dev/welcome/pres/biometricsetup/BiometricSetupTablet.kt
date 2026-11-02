package ru.kino.dev.welcome.pres.biometricsetup

import androidx.compose.runtime.Composable
import ru.kino.dev.welcome.pres.common.WelcomeCardFrame

/**
 * Offering biometrics in a wide window: the column of a phone on a card in the middle (pass-docs, Step 95). Ready for
 * when the desktop has biometrics - until then the creating screens skip this one there.
 */
@Composable
internal fun BiometricSetupActions.BiometricSetupTablet(state: BiometricSetupState) {
    WelcomeCardFrame {
        BiometricSetupForm(state)
    }
}
