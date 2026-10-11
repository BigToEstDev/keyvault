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
 * Crossing between the graphs slides by sides, not by the back stack: the closed database - `welcome` and
 * the locked screen - is on the left, the open one - `vault` - on the right. Into `vault` slides forward,
 * though unlocking pops back to it and opening pops the whole `welcome` graph; out of it slides back,
 * though locking pushes the locked screen (pass-docs, Step 34, bug 2 and item 5).
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
        enterTransition = { slideIntoContainer(direction(pop = false), tween(SLIDE_MILLIS)) },
        exitTransition = { slideOutOfContainer(direction(pop = false), tween(SLIDE_MILLIS)) },
        popEnterTransition = { slideIntoContainer(direction(pop = true), tween(SLIDE_MILLIS)) },
        popExitTransition = { slideOutOfContainer(direction(pop = true), tween(SLIDE_MILLIS)) },
    ) {
        welcomeGraph(navController, start)
        vaultGraph(navController)
    }
}

/**
 * Within a graph - by the back stack: forward for a new screen, back for a return. Across the graphs - by
 * the side the target is on: forward into `vault`, back out of it.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(pop: Boolean): SlideDirection {
    val forward = if (initialState.isInVault() != targetState.isInVault()) targetState.isInVault() else !pop
    return if (forward) SlideDirection.Start else SlideDirection.End
}

// The locked screen sits outside both graphs, on the side of `welcome`: the database is not usable there
private fun NavBackStackEntry.isInVault(): Boolean = destination.hierarchy.any { it.hasRoute(VaultGraph::class) }

private const val SLIDE_MILLIS = 300
