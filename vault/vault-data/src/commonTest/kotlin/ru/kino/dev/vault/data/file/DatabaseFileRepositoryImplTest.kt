package ru.kino.dev.vault.data.file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.Compression
import ru.kino.dev.core.DatabaseFile
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.KdbxVersion
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.data.changes.VaultChangesImpl
import ru.kino.dev.vault.file.DatabaseFileFailure
import ru.kino.dev.vault.file.DatabaseFileInfo

class DatabaseFileRepositoryImplTest {

    private val core = FakeNativeCore().apply {
        opened[DB] = OpenedDatabase(DB, "Passwords", null)
        databaseFile = DatabaseFile(name = "Passwords", version = KdbxVersion.V40, compression = Compression.GZip)
    }
    private val documents = FakeDatabaseFiles().apply { names[DB] = "Мои пароли.kdbx" }
    private val changes = VaultChangesImpl()
    private val repository = DatabaseFileRepositoryImpl(core, documents, changes)

    @Test
    fun `the file is the core's settings and the name the provider gives`() = runTest {
        assertEquals(
            DatabaseFileInfo(
                name = "Passwords",
                fileName = "Мои пароли.kdbx",
                version = KdbxVersion.V40,
                compression = Compression.GZip,
            ),
            repository.read(DB),
        )
    }

    @Test
    fun `a change goes to the core and nowhere else`() = runTest {
        repository.change(DB, "Home", Compression.None)

        assertEquals(DatabaseFile("Home", KdbxVersion.V40, Compression.None), core.databaseFile)
        assertEquals(listOf("changeDatabaseFile $DB"), core.journal)
    }

    @Test
    fun `a database closed meanwhile is unexpected`() = runTest {
        core.opened.remove(DB)

        expectFailure<DatabaseFileFailure.Unexpected> { repository.read(DB) }
        expectFailure<DatabaseFileFailure.Unexpected> { repository.change(DB, "Home", Compression.None) }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
