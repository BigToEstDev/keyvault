package ru.kino.dev.pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * The processors of the panes of a container, each pane in a store of its own: a pane gone - its processors are
 * cleared with it, the rest stay. Keyed by the pane, so the same screen for another entry is another processor.
 */
class PaneStores {

    private val owners = mutableMapOf<String, PaneOwner>()

    /** The owner of the processors of the pane [key]; the same one while the pane is kept. */
    fun owner(key: String): ViewModelStoreOwner = owners.getOrPut(key) { PaneOwner() }

    /** Keeps the panes of [keys], clears the rest; returns the keys of the cleared ones. */
    fun retain(keys: Set<String>): Set<String> {
        val gone = owners.keys - keys
        gone.forEach { key -> owners.remove(key)?.viewModelStore?.clear() }
        return gone
    }

    /** Clears every pane: the container is gone. */
    fun clear() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }

    private class PaneOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
}

/** [PaneStores] kept by the container's place on the back stack: they go when it is popped. */
internal class PaneStoresHolder : ViewModel() {
    val stores = PaneStores()

    override fun onCleared() {
        stores.clear()
    }
}
