package ru.kino.dev.welcome.pres.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreateDraftTest {

    @Test
    fun `the draft forgets the password when its graph goes`() {
        // The store stands for the back stack entry of the creation graph
        val store = ViewModelStore()
        val draft = ViewModelProvider.create(store, Factory)[CreateDraft::class]
        draft.start("pw", withKeyFile = true)
        draft.keyFileSaved(KEY)

        assertEquals("pw", draft.password)
        assertEquals(KEY, draft.keyFileUri)

        store.clear()

        assertNull(draft.password)
        assertNull(draft.keyFileUri)
    }

    @Test
    fun `starting again forgets a key file saved for the previous start`() {
        val draft = CreateDraft()
        draft.start("pw", withKeyFile = true)
        draft.keyFileSaved(KEY)

        draft.start("pw2", withKeyFile = false)

        assertNull(draft.keyFileUri)
    }

    private object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = CreateDraft() as T
    }

    private companion object {
        const val KEY = "content://keys/kdbxvault.keyx"
    }
}
