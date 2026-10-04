package ru.kino.dev.vault.pres.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.VaultGraph
import ru.kino.dev.vault.pres.entrieslist.EntriesListScreen

/** The screens of `vault` - the database is open. Entered at the list of entries, with its dbKey. */
fun NavGraphBuilder.vaultGraph() {
    navigation<VaultGraph>(startDestination = EntriesListRoute::class) {
        composable<EntriesListRoute> { entry ->
            EntriesListScreen(dbKey = entry.toRoute<EntriesListRoute>().dbKey)
        }
    }
}
