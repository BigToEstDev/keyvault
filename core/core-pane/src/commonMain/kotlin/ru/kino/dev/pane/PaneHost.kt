package ru.kino.dev.pane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.kino.dev.navigation.LocalNavigator

/**
 * The panes of one container: each with its own processors ([PaneStores]) and its own saved state. Made by
 * [rememberPaneHost]; a pane is drawn by [Pane].
 */
@Stable
class PaneHost internal constructor(
    private val stores: PaneStores,
    private val saveable: SaveableStateHolder,
) {

    /**
     * A pane: [content] - a screen as it is, its processor in the pane's store, its navigation through [router]
     * first. [key] - which pane: the same key, the same processor.
     */
    @Composable
    fun Pane(key: String, router: PaneRouter, content: @Composable () -> Unit) {
        val parent = LocalNavigator.current
        val currentRouter by rememberUpdatedState(router)
        val navigator = remember(key, parent) { PaneNavigator(parent) { currentRouter.route(it) } }
        val owner = remember(key) { stores.owner(key) }
        saveable.SaveableStateProvider(key) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalNavigator provides navigator,
                content = content,
            )
        }
    }

    internal fun retain(keys: Set<String>) {
        stores.retain(keys).forEach(saveable::removeState)
    }
}

/**
 * The [PaneHost] of the container drawn here: kept with the container's place on the back stack. [keys] - the panes
 * open now; the others are cleared, processors and saved state.
 */
@Composable
fun rememberPaneHost(keys: Set<String>): PaneHost {
    val stores = viewModel { PaneStoresHolder() }.stores
    val saveable = rememberSaveableStateHolder()
    val host = remember(stores, saveable) { PaneHost(stores, saveable) }
    // After the closed panes have left the composition
    LaunchedEffect(host, keys) { host.retain(keys) }
    return host
}
