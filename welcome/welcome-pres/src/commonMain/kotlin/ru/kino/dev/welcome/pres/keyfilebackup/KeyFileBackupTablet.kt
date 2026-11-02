package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.compose.runtime.Composable
import ru.kino.dev.welcome.pres.common.WelcomeCardFrame

/** The key file of a new database in a wide window: the column of a phone on a card in the middle (pass-docs, Step 95). */
@Composable
internal fun KeyFileBackupActions.KeyFileBackupTablet(state: KeyFileBackupState) {
    WelcomeCardFrame {
        KeyFileBackupForm(state)
    }
}
