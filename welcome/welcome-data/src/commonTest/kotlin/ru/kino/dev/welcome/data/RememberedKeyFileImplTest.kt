package ru.kino.dev.welcome.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.kino.dev.welcome.CurrentDatabase

class RememberedKeyFileImplTest {

    private val journal = mutableListOf<String>()
    private val store = FakeCurrentDatabaseStore(journal)
    private val remembered = RememberedKeyFileImpl(store, store)

    @Test
    fun `a new key file of the remembered database replaces the old one, the rest stays`() = runTest {
        store.current.value = REMEMBERED

        remembered.remember(DB, keyFileUri = "content://keys/new.keyx", keyFileName = "new.keyx")

        assertEquals(
            REMEMBERED.copy(keyFileUri = "content://keys/new.keyx", keyFileName = "new.keyx"),
            store.current.value,
        )
    }

    @Test
    fun `no key file any more is remembered as none`() = runTest {
        store.current.value = REMEMBERED

        remembered.remember(DB, keyFileUri = null, keyFileName = null)

        assertEquals(REMEMBERED.copy(keyFileUri = null, keyFileName = null), store.current.value)
    }

    @Test
    fun `another database is left alone`() = runTest {
        store.current.value = REMEMBERED

        remembered.remember("content://files/other.kdbx", keyFileUri = null, keyFileName = null)

        assertEquals(REMEMBERED, store.current.value)
        assertEquals(emptyList<String>(), journal)
    }

    @Test
    fun `with none remembered nothing is remembered`() = runTest {
        remembered.remember(DB, keyFileUri = "content://keys/new.keyx", keyFileName = "new.keyx")

        assertNull(store.current.value)
        assertEquals(emptyList<String>(), journal)
    }

    private companion object {
        const val DB = "content://files/passwords.kdbx"

        val REMEMBERED = CurrentDatabase(
            uri = DB,
            fileName = "passwords.kdbx",
            databaseName = "Passwords",
            keyFileUri = "content://keys/old.keyx",
            keyFileName = "old.keyx",
        )
    }
}
