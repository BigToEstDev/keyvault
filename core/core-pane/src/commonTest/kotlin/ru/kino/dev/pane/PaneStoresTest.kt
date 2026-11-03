package ru.kino.dev.pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PaneStoresTest {

    private val stores = PaneStores()

    @Test
    fun `a pane keeps its processor`() {
        assertSame(processorOf(stores.owner(ENTRY)), processorOf(stores.owner(ENTRY)))
    }

    @Test
    fun `another pane - another processor`() {
        assertNotSame(processorOf(stores.owner(ENTRY)), processorOf(stores.owner(OTHER)))
    }

    @Test
    fun `a pane gone - its processor cleared, the rest kept`() {
        val gone = processorOf(stores.owner(ENTRY))
        val kept = processorOf(stores.owner(OTHER))

        val cleared = stores.retain(setOf(OTHER))

        assertEquals(setOf(ENTRY), cleared)
        assertTrue(gone.cleared)
        assertFalse(kept.cleared)
        assertSame(kept, processorOf(stores.owner(OTHER)))
    }

    @Test
    fun `a pane opened again - a new processor`() {
        val first = processorOf(stores.owner(ENTRY))
        stores.retain(emptySet())

        assertNotSame(first, processorOf(stores.owner(ENTRY)))
    }

    @Test
    fun `the container gone - every processor cleared`() {
        val one = processorOf(stores.owner(ENTRY))
        val other = processorOf(stores.owner(OTHER))

        stores.clear()

        assertTrue(one.cleared)
        assertTrue(other.cleared)
    }

    private fun processorOf(owner: ViewModelStoreOwner): Processor =
        ViewModelProvider.create(owner, viewModelFactory { initializer { Processor() } })[Processor::class]

    private class Processor : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private companion object {
        const val ENTRY = "entry-1"
        const val OTHER = "entry-2"
    }
}
