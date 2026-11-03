package ru.kino.dev.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.kino.dev.pane.LayoutChangeEffect
import ru.kino.dev.settings.pres.navigation.settingsGraph
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.pres.navigation.vaultGraph
import ru.kino.dev.welcome.pres.navigation.welcomeGraph
import ru.kino.dev.welcome.pres.start.WelcomeStart

/**
 * The two graphs of the app: `welcome` while the database is not open, `vault` once it is. Screens slide in
 * from the end going forward and back out to it going back.
 *
 * Crossing between the graphs slides by sides, not by the back stack: the closed database - `welcome` and
 * the locked screen - is on the left, the open one - `vault` - on the right. Into `vault` slides forward,
 * though opening pops the whole `welcome` graph (pass-docs, Step 34, bug 2 and item 5). Locking and
 * unlocking do not slide: `vault` is not to be seen around the lock (Step 46).
 *
 * @param start decided once from the remembered database, before the first frame
 */
@Composable
fun AppNavHost(navigator: AppNavigator, autoLock: AutoLock, start: WelcomeStart) {
    val navController = rememberNavController()

    LaunchedEffect(navigator) {
        navigator.events.collect { event ->
            navController.handle(event)
        }
    }

    // Locked: the locked screen with nothing under it - `vault` is popped whole, its screens and what they
    // hold go, and unlocking starts at the root (pass-docs, Step 46). Not single top: every lock is a new
    // locked screen with a new processor, nothing typed for the one before (pass-docs, Step 40, fix 20)
    LaunchedEffect(autoLock) {
        autoLock.locked.collect { dbKey ->
            navController.handle(MviNavEvent.NavigateTo(LockedRoute(dbKey), popUpTo = VaultGraph, inclusive = true))
        }
    }

    // Across the line of the layout the stack of `vault` folds back to its root: on a wide window the root is the
    // container, holding the groups and the entries a phone keeps on the stack (pass-docs, Step 98). Outside `vault` -
    // nothing to fold
    LayoutChangeEffect { navController.popBackStack<VaultRootRoute>(inclusive = false) }

    val covering by autoLock.covering.collectAsStateWithLifecycle()
    val current by navController.currentBackStackEntryAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = WelcomeGraph,
            // Into the locked screen and back out of it the screens change at once: a slide would show
            // `vault` going away, or coming back, under the lock
            enterTransition = {
                if (autoLock.covering.value) EnterTransition.None
                else slideIntoContainer(direction(pop = false), tween(SLIDE_MILLIS))
            },
            exitTransition = {
                if (autoLock.covering.value) ExitTransition.None
                else slideOutOfContainer(direction(pop = false), tween(SLIDE_MILLIS))
            },
            popEnterTransition = { slideIntoContainer(direction(pop = true), tween(SLIDE_MILLIS)) },
            popExitTransition = { slideOutOfContainer(direction(pop = true), tween(SLIDE_MILLIS)) },
        ) {
            welcomeGraph(navController, start)
            vaultGraph { settingsGraph() }
        }
        // While the lock is on its way `vault` is not to be seen - the locked screen is, once it is up
        if (covering && current?.isInVault() == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
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
