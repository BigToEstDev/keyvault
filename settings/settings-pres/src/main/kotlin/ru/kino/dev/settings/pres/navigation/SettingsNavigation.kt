package ru.kino.dev.settings.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.settings.pres.settings.SettingsScreen

/**
 * The screens of `settings`. Built inside the graph of `vault` - the database is open there, and the lock
 * pops them with it (pass-docs, Step 52).
 */
fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> { SettingsScreen() }
}
