package ru.kino.dev.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.kino.dev.vault.pres.navigation.vaultGraph
import ru.kino.dev.welcome.pres.navigation.welcomeGraph
import ru.kino.dev.welcome.pres.start.WelcomeStart

/**
 * The two graphs of the app: `welcome` while the database is not open, `vault` once it is. Screens slide in
 * from the end going forward and back out to it going back.
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
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(SLIDE_MILLIS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(SLIDE_MILLIS)) },
    ) {
        welcomeGraph(navController, start)
        vaultGraph()
    }
}

private const val SLIDE_MILLIS = 300
