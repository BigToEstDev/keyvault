package ru.kino.dev.welcome.pres.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.pres.locked.LockedScreen
import ru.kino.dev.welcome.pres.start.WelcomeStart
import ru.kino.dev.welcome.pres.unlock.UnlockScreen
import ru.kino.dev.welcome.pres.welcome.WelcomeScreen

/**
 * The screens of `welcome`: its own graph, popped whole once the database is open, and the locked screen
 * next to it - that one is shown on top of `vault`, with the database still open under it.
 */
@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.welcomeGraph(navController: NavController, start: WelcomeStart) {
    navigation<WelcomeGraph>(
        startDestination = when (start) {
            WelcomeStart.Welcome -> WelcomeRoute
            WelcomeStart.Unlock -> UnlockRoute()
        },
    ) {
        composable<WelcomeRoute> { WelcomeScreen() }
        composable<UnlockRoute> { entry -> UnlockScreen(pickedUri = entry.toRoute<UnlockRoute>().pickedUri) }
    }
    composable<LockedRoute> { entry -> LockedScreen(dbKey = entry.toRoute<LockedRoute>().dbKey) }
}
