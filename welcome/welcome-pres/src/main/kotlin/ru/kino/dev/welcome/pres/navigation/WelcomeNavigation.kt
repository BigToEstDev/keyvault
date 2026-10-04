package ru.kino.dev.welcome.pres.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.pres.biometricsetup.BiometricSetupScreen
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.keyfilebackup.KeyFileBackupScreen
import ru.kino.dev.welcome.pres.locked.LockedScreen
import ru.kino.dev.welcome.pres.masterpassword.MasterPasswordScreen
import ru.kino.dev.welcome.pres.start.WelcomeStart
import ru.kino.dev.welcome.pres.unlock.UnlockScreen
import ru.kino.dev.welcome.pres.welcome.WelcomeScreen

/**
 * The screens of `welcome`: its own graph, popped whole once the database is open, and the locked screen
 * next to it - that one is shown on top of `vault`, with the database still open under it.
 *
 * @param start the remembered database decides it - nothing remembered starts on the welcome screen
 */
fun NavGraphBuilder.welcomeGraph(navController: NavController, start: WelcomeStart) {
    navigation<WelcomeGraph>(
        startDestination = when (start) {
            WelcomeStart.Welcome -> WelcomeRoute
            WelcomeStart.Unlock -> UnlockRoute()
        },
    ) {
        composable<WelcomeRoute> { WelcomeScreen() }
        composable<UnlockRoute> { entry -> UnlockScreen(pickedUri = entry.toRoute<UnlockRoute>().pickedUri) }

        navigation<CreateGraph>(startDestination = MasterPasswordRoute) {
            composable<MasterPasswordRoute> { entry ->
                MasterPasswordScreen(draft = createDraft(navController, entry))
            }
            composable<KeyFileBackupRoute> { entry ->
                KeyFileBackupScreen(draft = createDraft(navController, entry))
            }
        }
        composable<BiometricSetupRoute> { entry ->
            BiometricSetupScreen(dbKey = entry.toRoute<BiometricSetupRoute>().dbKey)
        }
    }
    composable<LockedRoute> { entry -> LockedScreen(dbKey = entry.toRoute<LockedRoute>().dbKey) }
}

/** The draft of the creation graph - one for all its screens, gone with the graph. */
@Composable
private fun createDraft(navController: NavController, entry: NavBackStackEntry): CreateDraft {
    val graphEntry = remember(entry) { navController.getBackStackEntry<CreateGraph>() }
    return hiltViewModel(graphEntry)
}
