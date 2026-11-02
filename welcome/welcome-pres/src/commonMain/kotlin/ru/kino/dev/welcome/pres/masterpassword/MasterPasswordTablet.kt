package ru.kino.dev.welcome.pres.masterpassword

import androidx.compose.runtime.Composable
import ru.kino.dev.welcome.pres.common.WelcomeCardFrame

/** Setting the master password in a wide window: the column of a phone on a card in the middle (pass-docs, Step 95). */
@Composable
internal fun MasterPasswordActions.MasterPasswordTablet(state: MasterPasswordState) {
    WelcomeCardFrame {
        MasterPasswordForm(state)
    }
}
