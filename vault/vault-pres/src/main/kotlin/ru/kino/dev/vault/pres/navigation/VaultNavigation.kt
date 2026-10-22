package ru.kino.dev.vault.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.navigation.VaultGraph
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.pres.entryedit.EntryEditScreen
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryScreen
import ru.kino.dev.vault.pres.entryread.EntryReadScreen
import ru.kino.dev.vault.pres.entryversion.EntryVersionScreen
import ru.kino.dev.vault.pres.grouplist.GroupListScreen
import ru.kino.dev.vault.pres.trash.TrashScreen
import ru.kino.dev.vault.pres.trashentry.TrashEntryScreen

/**
 * The screens of `vault` - the database is open. Entered at its root, with its dbKey; every group below is
 * the same screen again, one place on the back stack per level.
 *
 * @param nested the screens of other modules that belong to the open database - the settings: inside this
 * graph the lock pops them with `vault`, and `vault` does not depend on those modules (pass-docs, Step 52)
 */
fun NavGraphBuilder.vaultGraph(nested: NavGraphBuilder.() -> Unit = {}) {
    navigation<VaultGraph>(startDestination = VaultRootRoute::class) {
        composable<VaultRootRoute> { entry ->
            GroupListScreen(dbKey = entry.toRoute<VaultRootRoute>().dbKey, groupId = null)
        }
        composable<GroupRoute> { entry ->
            val route = entry.toRoute<GroupRoute>()
            GroupListScreen(dbKey = route.dbKey, groupId = route.groupId)
        }
        composable<EntryReadRoute> { entry ->
            val route = entry.toRoute<EntryReadRoute>()
            EntryReadScreen(dbKey = route.dbKey, entryId = route.entryId)
        }
        composable<EntryHistoryRoute> { entry ->
            val route = entry.toRoute<EntryHistoryRoute>()
            EntryHistoryScreen(dbKey = route.dbKey, entryId = route.entryId)
        }
        composable<EntryVersionRoute> { entry ->
            val route = entry.toRoute<EntryVersionRoute>()
            EntryVersionScreen(dbKey = route.dbKey, entryId = route.entryId, index = route.index)
        }
        composable<EntryCreateRoute> { entry ->
            val route = entry.toRoute<EntryCreateRoute>()
            EntryEditScreen(dbKey = route.dbKey, groupId = route.groupId, entryId = null)
        }
        composable<EntryEditRoute> { entry ->
            val route = entry.toRoute<EntryEditRoute>()
            EntryEditScreen(dbKey = route.dbKey, groupId = null, entryId = route.entryId)
        }
        composable<TrashRoute> { entry -> TrashScreen(dbKey = entry.toRoute<TrashRoute>().dbKey) }
        composable<TrashEntryRoute> { entry ->
            val route = entry.toRoute<TrashEntryRoute>()
            TrashEntryScreen(dbKey = route.dbKey, entryId = route.entryId)
        }
        nested()
    }
}
