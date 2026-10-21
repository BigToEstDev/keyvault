package ru.kino.dev.settings.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.settings.pres.appearance.AppearanceScreen
import ru.kino.dev.settings.pres.lock.LockScreen
import ru.kino.dev.settings.pres.privacy.PrivacyScreen
import ru.kino.dev.settings.pres.settings.SettingsScreen

/**
 * The screens of `settings`. Built inside the graph of `vault` - the database is open there, and the lock
 * pops them with it (pass-docs, Step 52).
 */
fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> { entry -> SettingsScreen(dbKey = entry.toRoute<SettingsRoute>().dbKey) }
    composable<AppearanceRoute> { AppearanceScreen() }
    composable<LockRoute> { entry -> LockScreen(dbKey = entry.toRoute<LockRoute>().dbKey) }
    composable<PrivacyRoute> { PrivacyScreen() }
}
