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
import org.orbitmvi.orbit.compose.collectAsState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
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
import ru.kino.dev.ui.components.SwitchRow

/**
 * How the app looks: the group "Theme" - the theme of the app and the color theme to come (pass-docs, Step 55);
 * the group "Language" (Step 57); the group "Entries" (Step 58).
 */
@Composable
internal fun AppearanceScreen(
    dbKey: String,
    processor: AppearanceProcessor = koinViewModel { parametersOf(dbKey) },
) {
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
                            enabled = state.themeMode != null,
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
                            enabled = state.language != null,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                item(key = "entries-header") {
                    SettingsSectionHeader(title = stringResource(R.string.settings_entries), top = 8.dp)
                }
                item(key = "password-coloring") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_password_coloring),
                            text = stringResource(R.string.settings_password_coloring_hint),
                            // Until read nothing is chosen for real: the switch is disabled meanwhile, shown on
                            checked = state.passwordColoring ?: true,
                            onCheckedChange = { processor.action(AppearanceAction.SetPasswordColoring(it)) },
                            enabled = state.passwordColoring != null,
                        )
                    }
                }
                item(key = "password-strength") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_password_strength),
                            text = stringResource(R.string.settings_password_strength_hint),
                            // Until read nothing is chosen for real: the switch is disabled meanwhile, shown off
                            checked = state.passwordStrength ?: false,
                            onCheckedChange = { processor.action(AppearanceAction.SetPasswordStrength(it)) },
                            enabled = state.passwordStrength != null,
                        )
                    }
                }
                item(key = "user-name-in-list") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_user_name_in_list),
                            // The open database hides user names: off, whatever the app chose (pass-docs, Step 56, item 5)
                            text = stringResource(
                                if (state.userNameHidden) {
                                    R.string.settings_user_name_hidden_hint
                                } else {
                                    R.string.settings_user_name_in_list_hint
                                },
                            ),
                            checked = !state.userNameHidden && state.userNameInList == true,
                            onCheckedChange = { processor.action(AppearanceAction.SetUserNameInList(it)) },
                            enabled = !state.userNameHidden && state.userNameInList != null,
                        )
                    }
                }
                item(key = "entry-count") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_entry_count),
                            text = stringResource(R.string.settings_entry_count_hint),
                            checked = state.entryCount ?: true,
                            onCheckedChange = { processor.action(AppearanceAction.SetEntryCount(it)) },
                            enabled = state.entryCount != null,
                        )
                    }
                }
                item(key = "entry-count-nested") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_entry_count_nested),
                            text = stringResource(R.string.settings_entry_count_nested_hint),
                            checked = state.entryCountNested ?: true,
                            onCheckedChange = { processor.action(AppearanceAction.SetEntryCountNested(it)) },
                            // Nothing to count without the number
                            enabled = state.entryCountNested != null && state.entryCount == true,
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
