package ru.kino.dev.settings.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.settings.pres.appearance.AppearanceScreen
import ru.kino.dev.settings.pres.entries.EntriesScreen
import ru.kino.dev.settings.pres.file.FileScreen
import ru.kino.dev.settings.pres.lock.LockScreen
import ru.kino.dev.settings.pres.presetedit.PresetEditScreen
import ru.kino.dev.settings.pres.presets.PresetsScreen
import ru.kino.dev.settings.pres.privacy.PrivacyScreen
import ru.kino.dev.settings.pres.settings.SettingsScreen

/**
 * The screens of `settings`. Built inside the graph of `vault` - the database is open there, and the lock
 * pops them with it (pass-docs, Step 52).
 */
fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> { entry -> SettingsScreen(dbKey = entry.toRoute<SettingsRoute>().dbKey) }
    composable<AppearanceRoute> { entry -> AppearanceScreen(dbKey = entry.toRoute<AppearanceRoute>().dbKey) }
    composable<LockRoute> { entry -> LockScreen(dbKey = entry.toRoute<LockRoute>().dbKey) }
    composable<PrivacyRoute> { PrivacyScreen() }
    composable<PresetsRoute> { PresetsScreen() }
    composable<FileRoute> { entry -> FileScreen(dbKey = entry.toRoute<FileRoute>().dbKey) }
    composable<EntriesRoute> { entry -> EntriesScreen(dbKey = entry.toRoute<EntriesRoute>().dbKey) }
    composable<PresetEditRoute> { entry -> PresetEditScreen(presetId = entry.toRoute<PresetEditRoute>().presetId) }
}
