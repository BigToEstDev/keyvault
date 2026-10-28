package ru.kino.dev.settings.pres.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import org.koin.compose.viewmodel.koinViewModel
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow

/** What the app lets out of its screen: the group "Data protection" - allowing screenshots (pass-docs, Step 61). */
@Composable
internal fun PrivacyScreen(processor: PrivacyProcessor = koinViewModel()) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.action(PrivacyAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                PrivacyMessage.NotSaved -> R.string.settings_message_not_saved
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(PrivacyAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_privacy),
                    onBack = { processor.action(PrivacyAction.Back) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "privacy") { SettingsSectionHeader(title = stringResource(R.string.settings_privacy)) }
                item(key = "allow-screenshots") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_allow_screenshots),
                            text = stringResource(R.string.settings_allow_screenshots_hint),
                            checked = state.allowScreenshots ?: false,
                            onCheckedChange = { processor.action(PrivacyAction.SetAllowScreenshots(it)) },
                            // Until read nothing is chosen for real: the row is disabled meanwhile
                            enabled = state.allowScreenshots != null,
                        )
                    }
                }
            }
        }
    }
}
