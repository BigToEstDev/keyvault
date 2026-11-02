package ru.kino.dev.welcome.pres.biometricsetup

import androidx.compose.runtime.Composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/** Offering biometrics after creating: the layout picked by the width of the window (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun BiometricSetupActions.BiometricSetupContent(state: BiometricSetupState) {
    when (LocalLayoutType.current) {
        LayoutType.Mobile -> BiometricSetupMobile(state)
        LayoutType.Tablet -> BiometricSetupTablet(state)
    }
}
