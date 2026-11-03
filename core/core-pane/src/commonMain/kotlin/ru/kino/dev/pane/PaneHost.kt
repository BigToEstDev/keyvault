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
import kotlinx.coroutines.channels.Channel
import ru.kino.dev.navigation.LocalNavigator
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.ui.components.LocalPaneBack
import ru.kino.dev.ui.components.LocalScreenBackdrop
import ru.kino.dev.ui.components.PaneBack

/**
 * The panes of one container: each with its own processors ([PaneStores]) and its own saved state. Made by
 * [rememberPaneHost]; a pane is drawn by [Pane].
 */
@Stable
class PaneHost internal constructor(
    private val stores: PaneStores,
    private val saveable: SaveableStateHolder,
) {

    private val backs = mutableMapOf<String, PaneBackSlot>()

    /**
     * A pane: [content] - a screen as it is, its processor in the pane's store, its navigation through [router]
     * first, on the card of the container - no backdrop of its own. [key] - which pane: the same key, the same
     * processor.
     */
    @Composable
    fun Pane(key: String, router: PaneRouter, content: @Composable () -> Unit) {
        val parent = LocalNavigator.current
        val currentRouter by rememberUpdatedState(router)
        val navigator = remember(key, parent) { PaneNavigator(parent) { currentRouter.route(it) } }
        val owner = remember(key) { stores.owner(key) }
        val back = remember(key) { backs.getOrPut(key, ::PaneBackSlot) }
        saveable.SaveableStateProvider(key) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalNavigator provides navigator,
                LocalScreenBackdrop provides false,
                LocalPaneBack provides back.handlers,
                content = content,
            )
            // After the screen has registered its "back": asked before the pane was drawn, it is done now
            LaunchedEffect(back, navigator) {
                for (request in back.requests) {
                    if (!back.handlers.handle()) navigator.navigate(MviNavEvent.NavigateBack)
                }
            }
        }
    }

    /**
     * "Back" of the pane [key], as its screen would go back (pass-docs, Step 98, item 6): the screen asks before
     * leaving ([ru.kino.dev.ui.components.BackHandler]) - it asks; it does not - "back" to its navigation. The pane not
     * drawn yet - done once it is.
     */
    fun back(key: String) {
        backs.getOrPut(key, ::PaneBackSlot).requests.trySend(Unit)
    }

    internal fun retain(keys: Set<String>) {
        stores.retain(keys).forEach(saveable::removeState)
        backs.keys.retainAll(keys)
    }
}

/** The "back" of one pane: what its screen registered, and the requests of the container not done yet. */
private class PaneBackSlot {
    val handlers = PaneBack()
    val requests = Channel<Unit>(Channel.CONFLATED)
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
