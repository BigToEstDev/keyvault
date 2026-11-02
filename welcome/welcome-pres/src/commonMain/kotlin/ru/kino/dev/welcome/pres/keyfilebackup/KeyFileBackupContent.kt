package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.compose.runtime.Composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/** The key file of a new database: the layout picked by the width of the window (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun KeyFileBackupActions.KeyFileBackupContent(state: KeyFileBackupState) {
    when (LocalLayoutType.current) {
        LayoutType.Mobile -> KeyFileBackupMobile(state)
        LayoutType.Tablet -> KeyFileBackupTablet(state)
    }
}
