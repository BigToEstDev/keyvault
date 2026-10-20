package ru.kino.dev.settings.pres.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode
import ru.kino.dev.settings.pres.settings.components.SettingsCard
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.Dropdown
import ru.kino.dev.ui.components.DropdownOption
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.RadioGroup
import ru.kino.dev.ui.components.SnackbarMessage

/**
 * How the app looks: the group "Theme" - the theme of the app and the color theme to come (pass-docs, Step 55);
 * the group "Language" (Step 57).
 */
@Composable
internal fun AppearanceScreen(processor: AppearanceProcessor = hiltViewModel()) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.action(AppearanceAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                AppearanceMessage.NotSaved -> R.string.settings_message_not_saved
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(AppearanceAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_appearance),
                    onBack = { processor.action(AppearanceAction.Back) },
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
                item(key = "theme") { SettingsSectionHeader(title = stringResource(R.string.settings_theme)) }
                item(key = "mode") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        RadioGroup(
                            title = stringResource(R.string.settings_theme_mode),
                            options = ThemeMode.entries,
                            // Until read nothing is chosen for real: the choice is disabled meanwhile
                            selected = state.themeMode ?: ThemeMode.System,
                            label = { stringResource(it.label) },
                            onSelect = { processor.action(AppearanceAction.SelectTheme(it)) },
                            enabled = state.themeMode != null && !state.saving,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                // Choosing a color theme comes later: the card is there, but takes no taps (pass-docs, Step 55, item 2)
                item(key = "colors") {
                    SettingsCard(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_color_theme),
                        text = stringResource(R.string.settings_coming_soon),
                        enabled = false,
                    )
                }
                item(key = "language-header") {
                    // Set apart from the group above, as the sections of the main screen
                    SettingsSectionHeader(title = stringResource(R.string.settings_language), top = 8.dp)
                }
                item(key = "language") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Dropdown(
                            selected = state.language,
                            options = AppLanguage.entries.map { DropdownOption(it, stringResource(it.label)) },
                            onSelect = { processor.action(AppearanceAction.SelectLanguage(it)) },
                            label = stringResource(R.string.settings_language),
                            // Until read nothing is chosen for real: the choice is disabled meanwhile
                            enabled = state.language != null && !state.saving,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    }

private val AppLanguage.label: Int
    get() = when (this) {
        AppLanguage.System -> R.string.settings_language_system
        AppLanguage.English -> R.string.settings_language_english
        AppLanguage.Russian -> R.string.settings_language_russian
    }
