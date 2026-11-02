package ru.kino.dev.welcome.pres.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.ui.res.stringResource

// The pieces of the layouts of the welcome screen (pass-docs, Step 95): the layouts place them

/**
 * Comes into view once, when the screen is first shown: [appeared] is kept above the layouts, so a resize of the
 * window or a turn of the phone across the line of [ru.kino.dev.ui.theme.LayoutType] does not show it again.
 */
@Composable
internal fun WelcomeAppear(appeared: MutableTransitionState<Boolean>, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = appeared,
        enter = fadeIn(tween(APPEAR_MILLIS)) + slideInVertically(tween(APPEAR_MILLIS)) { it / 8 },
    ) {
        content()
    }
}

/** The shield, what the app is, and the two ways on - centred. */
@Composable
internal fun WelcomeActions.WelcomeForm() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShieldImage(modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        PrimaryButton(text = stringResource(Res.string.welcome_open), onClick = ::onOpen)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = stringResource(Res.string.action_create_database), onClick = ::onCreate)
    }
}

/** The state of [WelcomeAppear] - to be shown once, from the first time on. */
@Composable
internal fun rememberWelcomeAppeared(): MutableTransitionState<Boolean> =
    remember { MutableTransitionState(false).apply { targetState = true } }

private const val APPEAR_MILLIS = 600
