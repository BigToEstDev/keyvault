package ru.kino.dev.welcome.pres.welcome

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import ru.kino.dev.welcome.pres.common.WelcomeCardFrame

/**
 * The first screen in a wide window: the column of a phone on a card in the middle (pass-docs, Step 95); the card
 * stands, what is on it comes into view.
 */
@Composable
internal fun WelcomeActions.WelcomeTablet(appeared: MutableTransitionState<Boolean>) {
    WelcomeCardFrame {
        WelcomeAppear(appeared) {
            WelcomeForm()
        }
    }
}
