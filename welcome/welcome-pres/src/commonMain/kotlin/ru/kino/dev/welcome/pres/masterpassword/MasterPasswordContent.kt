package ru.kino.dev.welcome.pres.masterpassword

import androidx.compose.runtime.Composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/** Setting the master password: the layout picked by the width of the window (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun MasterPasswordActions.MasterPasswordContent(state: MasterPasswordState) {
    when (LocalLayoutType.current) {
        LayoutType.Mobile -> MasterPasswordMobile(state)
        LayoutType.Tablet -> MasterPasswordTablet(state)
    }
}
