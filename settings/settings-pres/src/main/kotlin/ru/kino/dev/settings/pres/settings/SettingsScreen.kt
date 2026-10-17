package ru.kino.dev.settings.pres.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground

/** The settings: for now the top bar alone (pass-docs, Step 52). */
@Composable
internal fun SettingsScreen(processor: SettingsProcessor = hiltViewModel()) {
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_title),
                    onBack = { processor.action(SettingsAction.Back) },
                )
            },
        ) { padding ->
            // The settings come here, under the top bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}
