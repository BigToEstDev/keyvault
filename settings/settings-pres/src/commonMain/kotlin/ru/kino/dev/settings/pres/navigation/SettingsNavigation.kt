package ru.kino.dev.settings.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.pane.adaptiveComposable
import ru.kino.dev.settings.pres.appearance.AppearanceScreen
import ru.kino.dev.settings.pres.entries.EntriesScreen
import ru.kino.dev.settings.pres.file.FileScreen
import ru.kino.dev.settings.pres.lock.LockScreen
import ru.kino.dev.settings.pres.presetedit.PresetEditScreen
import ru.kino.dev.settings.pres.presetread.PresetReadScreen
import ru.kino.dev.settings.pres.presets.PresetsScreen
import ru.kino.dev.settings.pres.presetscontainer.PresetsContainerScreen
import ru.kino.dev.settings.pres.privacy.PrivacyScreen
import ru.kino.dev.settings.pres.security.SecurityScreen
import ru.kino.dev.settings.pres.settings.SettingsScreen
import ru.kino.dev.settings.pres.settingscontainer.SettingsContainerScreen

/**
 * The screens of `settings`. Built inside the graph of `vault` - the database is open there, and the lock
 * pops them with it (pass-docs, Step 52).
 */
fun NavGraphBuilder.settingsGraph() {
    // On a wide window the settings are a container: the main screen with the page chosen beside it (Step 101)
    adaptiveComposable<SettingsRoute>(
        mobile = { entry -> SettingsScreen(dbKey = entry.toRoute<SettingsRoute>().dbKey) },
        tablet = { entry -> SettingsContainerScreen(dbKey = entry.toRoute<SettingsRoute>().dbKey) },
    )
    composable<AppearanceRoute> { entry -> AppearanceScreen(dbKey = entry.toRoute<AppearanceRoute>().dbKey) }
    composable<LockRoute> { entry -> LockScreen(dbKey = entry.toRoute<LockRoute>().dbKey) }
    composable<PrivacyRoute> { PrivacyScreen() }
    // On a wide window the presets are a container: their list with the presets open beside it (Step 100, item 3)
    adaptiveComposable<PresetsRoute>(mobile = { PresetsScreen() }, tablet = { PresetsContainerScreen() })
    // On a wide window the container opens it in a tab: on the stack only on a phone (Step 99, item 8)
    composable<PresetReadRoute> { entry -> PresetReadScreen(presetId = entry.toRoute<PresetReadRoute>().presetId) }
    composable<FileRoute> { entry -> FileScreen(dbKey = entry.toRoute<FileRoute>().dbKey) }
    composable<EntriesRoute> { entry -> EntriesScreen(dbKey = entry.toRoute<EntriesRoute>().dbKey) }
    composable<SecurityRoute> { entry -> SecurityScreen(dbKey = entry.toRoute<SecurityRoute>().dbKey) }
    // A preset from outside the presets - "a new preset" from the form of an entry: on a wide window the container,
    // with its tab; leaving it - back to the form
    adaptiveComposable<PresetEditRoute>(
        mobile = { entry ->
            val route = entry.toRoute<PresetEditRoute>()
            PresetEditScreen(presetId = route.presetId, readSaved = route.readSaved)
        },
        tablet = { entry -> PresetsContainerScreen(opening = entry.toRoute<PresetEditRoute>()) },
    )
}
