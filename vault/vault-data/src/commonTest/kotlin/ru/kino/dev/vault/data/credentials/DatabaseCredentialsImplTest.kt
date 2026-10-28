package ru.kino.dev.vault.data.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure

class DatabaseCredentialsImplTest {

    private val core = FakeNativeCore().apply {
        opened[DB] = OpenedDatabase(DB, "Passwords", "passwords.kdbx", null)
        password = "pw"
    }
    private val credentials = DatabaseCredentialsImpl(core)

    @Test
    fun `the password of the database is checked by the core`() = runTest {
        assertTrue(credentials.verifyPassword(DB, "pw"))
        assertFalse(credentials.verifyPassword(DB, "other"))
        assertEquals(listOf("verifyPassword $DB", "verifyPassword $DB"), core.journal)
    }

    @Test
    fun `a key with a key file alone has no password`() = runTest {
        assertTrue(credentials.hasPassword(DB))

        core.credentials = CredentialsUsed(password = false, keyFile = true)

        assertFalse(credentials.hasPassword(DB))
    }

    @Test
    fun `a database closed meanwhile is the failure of the core`() = runTest {
        core.opened.remove(DB)

        expectFailure<CoreException> { credentials.verifyPassword(DB, "pw") }
        expectFailure<CoreException> { credentials.hasPassword(DB) }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
