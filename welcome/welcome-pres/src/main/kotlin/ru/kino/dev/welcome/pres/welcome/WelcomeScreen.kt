package ru.kino.dev.welcome.pres.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.welcome.pres.common.ShieldImage
import ru.kino.dev.ui.files.rememberOpenDocument

@Composable
internal fun WelcomeScreen(processor: WelcomeProcessor = hiltViewModel()) {
    val pickDatabase = rememberOpenDocument { processor.action(WelcomeAction.DatabasePicked(it)) }

    processor.collectSideEffect { effect ->
        when (effect) {
            WelcomeSideEffect.PickDatabase -> pickDatabase()
        }
    }

    WelcomeContent(
        onOpen = { processor.action(WelcomeAction.OpenClicked) },
        onCreate = { processor.action(WelcomeAction.CreateClicked) },
    )
}

@Composable
private fun WelcomeContent(onOpen: () -> Unit, onCreate: () -> Unit) {
    // Appears once, when the screen is first shown - not again after rotation
    val appeared = remember { MutableTransitionState(false).apply { targetState = true } }

    GradientBackground {
        AnimatedVisibility(
            visibleState = appeared,
            enter = fadeIn(tween(APPEAR_MILLIS)) + slideInVertically(tween(APPEAR_MILLIS)) { it / 8 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShieldImage(modifier = Modifier.size(140.dp))
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(48.dp))
                PrimaryButton(text = stringResource(R.string.welcome_open), onClick = onOpen)
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = stringResource(R.string.welcome_create), onClick = onCreate)
            }
        }
    }
}

private const val APPEAR_MILLIS = 600
