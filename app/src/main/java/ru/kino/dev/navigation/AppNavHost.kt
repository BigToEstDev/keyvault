package ru.kino.dev.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.kino.dev.vault.pres.navigation.vaultGraph
import ru.kino.dev.welcome.pres.navigation.welcomeGraph
import ru.kino.dev.welcome.pres.start.WelcomeStart

/**
 * The two graphs of the app: `welcome` while the database is not open, `vault` once it is. Screens slide in
 * from the end going forward and back out to it going back.
 *
 * Crossing between the graphs always slides forward, whichever way the back stack moves: unlocking on the
 * locked screen pops back to `vault`, and opening a database pops the whole `welcome` graph - both are a
 * step forward for the user, not a return (pass-docs, Step 34, bug 2).
 *
 * @param start decided once from the remembered database, before the first frame
 */
@Composable
fun AppNavHost(navigator: Navigator, start: WelcomeStart) {
    val navController = rememberNavController()

    LaunchedEffect(navigator) {
        navigator.events.collect { event ->
            navController.handle(event)
        }
    }

    NavHost(
        navController = navController,
        startDestination = WelcomeGraph,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(SLIDE_MILLIS)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(SLIDE_MILLIS)) },
        popEnterTransition = { slideIntoContainer(popDirection(), tween(SLIDE_MILLIS)) },
        popExitTransition = { slideOutOfContainer(popDirection(), tween(SLIDE_MILLIS)) },
    ) {
        welcomeGraph(navController, start)
        vaultGraph()
    }
}

/** Back slides back - unless it crosses between the graphs, which is a step forward. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.popDirection(): SlideDirection =
    if (initialState.isInVault() != targetState.isInVault()) SlideDirection.Start else SlideDirection.End

// The locked screen sits outside both graphs, on the side of `welcome`: the database is not usable there
private fun NavBackStackEntry.isInVault(): Boolean = destination.hierarchy.any { it.hasRoute(VaultGraph::class) }

private const val SLIDE_MILLIS = 300
