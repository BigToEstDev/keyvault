package ru.kino.dev.welcome.pres.welcome

import androidx.compose.runtime.Composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/** The first screen: the layout picked by the width of the window (pass-docs, `docs/app/mvi-arc.md`). */
@Composable
internal fun WelcomeActions.WelcomeContent() {
    // Above the layouts: the screen comes into view once, whichever layout it is in
    val appeared = rememberWelcomeAppeared()
    when (LocalLayoutType.current) {
        LayoutType.Mobile -> WelcomeMobile(appeared)
        LayoutType.Tablet -> WelcomeTablet(appeared)
    }
}
