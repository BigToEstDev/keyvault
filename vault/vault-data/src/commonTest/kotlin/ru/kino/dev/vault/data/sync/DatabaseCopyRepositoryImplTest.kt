package ru.kino.dev.vault.data.sync

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FakeSharedFiles
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.sync.DatabaseCopyFailure

class DatabaseCopyRepositoryImplTest {

    private val documents = FakeDatabaseFiles()
    private val shared = FakeSharedFiles()
    private val repository = DatabaseCopyRepositoryImpl(documents, shared)

    @Test
    fun `the file of the database is read as it is`() = runTest {
        documents.contents[DB] = BYTES

        assertContentEquals(BYTES, repository.read(DB))
    }

    @Test
    fun `a file that cannot be read is unreadable`() = runTest {
        documents.unreachable += DB

        expectFailure<DatabaseCopyFailure.Unreadable> { repository.read(DB) }
    }

    @Test
    fun `the copy is written over the file picked`() = runTest {
        repository.write(FILE, BYTES)

        assertContentEquals(BYTES, documents.contents[FILE])
    }

    @Test
    fun `a copy that cannot be written is not written`() = runTest {
        documents.unwritable += FILE

        expectFailure<DatabaseCopyFailure.NotWritten> { repository.write(FILE, BYTES) }
    }

    @Test
    fun `the name is the provider's, none when it fails`() = runTest {
        documents.names[DB] = "base.kdbx"
        assertEquals("base.kdbx", repository.name(DB))

        assertNull(repository.name(FILE))
    }

    @Test
    fun `a file is shared through the platform`() = runTest {
        val uri = repository.prepareShare("base.kdbx", BYTES)

        assertEquals("${FakeSharedFiles.URI_PREFIX}base.kdbx", uri)
        assertContentEquals(BYTES, shared.files["base.kdbx"])
    }

    @Test
    fun `a file that cannot be put to share is not written`() = runTest {
        shared.failure = DatabaseFileException("no room")

        expectFailure<DatabaseCopyFailure.NotWritten> { repository.prepareShare("base.kdbx", BYTES) }
    }

    @Test
    fun `sharing is there as the platform says`() {
        shared.isAvailable = false

        assertFalse(repository.isShareAvailable())
    }

    private companion object {
        const val DB = "content://db"
        const val FILE = "content://copy"
        val BYTES = byteArrayOf(1, 2, 3)
    }
}
