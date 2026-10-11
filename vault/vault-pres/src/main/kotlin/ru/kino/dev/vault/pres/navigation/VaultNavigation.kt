package ru.kino.dev.vault.pres.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kino.dev.navigation.VaultGraph
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.pres.entryedit.EntryEditScreen
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen

/**
 * The screens of `vault` - the database is open. Entered at its root, with its dbKey; every group below is
 * the same screen again, one place on the back stack per level.
 *
 * The entry screens are stubs with no processor yet, so their "back" goes to [navController] straight.
 */
fun NavGraphBuilder.vaultGraph(navController: NavController) {
    navigation<VaultGraph>(startDestination = VaultRootRoute::class) {
        composable<VaultRootRoute> { entry ->
            GroupListScreen(dbKey = entry.toRoute<VaultRootRoute>().dbKey, groupId = null)
        }
        composable<GroupRoute> { entry ->
            val route = entry.toRoute<GroupRoute>()
            GroupListScreen(dbKey = route.dbKey, groupId = route.groupId)
        }
        composable<EntryReadRoute> {
            EntryReadScreen(onBack = { navController.popBackStack() })
        }
        composable<EntryEditRoute> {
            EntryEditScreen(onBack = { navController.popBackStack() })
        }
    }
}
