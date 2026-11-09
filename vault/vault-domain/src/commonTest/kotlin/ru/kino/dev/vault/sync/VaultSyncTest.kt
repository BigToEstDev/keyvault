package ru.kino.dev.vault.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.lock.DatabaseGuard

class VaultSyncTest {

    private val database = FakeVaultDatabase()
    private val status = SaveStatus()
    private val guard = DatabaseGuard()
    private val sync = VaultSync(database, status, guard)

    @Test
    fun `synchronizing merges the file and saves, reporting what came in`() = runTest {
        val summary = sync.synchronize(DB)

        assertSame(database.mergeSummary, summary)
        assertEquals(listOf("merge", "save"), database.calls)
    }

    @Test
    fun `a file that cannot be merged is not saved over`() = runTest {
        database.mergeFailure = MergeFailure.CredentialsChanged()

        expectFailure<MergeFailure.CredentialsChanged> { sync.synchronize(DB) }

        assertEquals(listOf("merge"), database.calls)
    }

    @Test
    fun `a merge not written is said to be not written`() = runTest {
        database.saveFailure = DatabaseFileException("disk full")

        expectFailure<MergeFailure.NotSaved> { sync.synchronize(DB) }
    }

    @Test
    fun `saving again after a damaged write says it is written`() = runTest {
        status.damaged(DB)
        database.onSave = { status.saved(DB) }

        assertTrue(sync.saveAgain(DB))
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `saving again that fails says it is not written`() = runTest {
        status.damaged(DB)
        database.saveFailure = DatabaseFileException("offline")

        assertFalse(sync.saveAgain(DB))
    }

    @Test
    fun `a sync waits for an edit being written, and a lock waits for the sync`() = runTest {
        val written = CompletableDeferred<Unit>()
        launch(start = CoroutineStart.UNDISPATCHED) { guard.exclusive { written.await() } }
        val syncing = launch(start = CoroutineStart.UNDISPATCHED) { sync.synchronize(DB) }

        assertTrue(database.calls.isEmpty())
        written.complete(Unit)
        syncing.join()
        assertEquals(listOf("merge", "save"), database.calls)
    }

    @Test
    fun `the quiet check merges a file written elsewhere and saves`() = runTest {
        database.changedElsewhere = true

        val summary = sync.check(DB)

        assertSame(database.mergeSummary, summary)
        assertEquals(listOf("hasChangedElsewhere", "merge", "save"), database.calls)
    }

    @Test
    fun `the quiet check leaves a file that is ours`() = runTest {
        assertNull(sync.check(DB))

        assertEquals(listOf("hasChangedElsewhere"), database.calls)
    }

    @Test
    fun `the quiet check does not merge a change left after Cancel`() = runTest {
        database.changedElsewhere = true
        status.changedElsewhere(DB)

        assertNull(sync.check(DB))

        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `the quiet check does not merge a change whose write failed`() = runTest {
        database.changedElsewhere = true
        status.notSaved(DB)

        assertNull(sync.check(DB))

        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a file that cannot be read now is not a failure of the quiet check`() = runTest {
        database.checkFailure = DatabaseFileException("offline")

        assertNull(sync.check(DB))
    }

    @Test
    fun `the quiet check says when the file cannot be merged`() = runTest {
        database.changedElsewhere = true
        database.mergeFailure = MergeFailure.CredentialsChanged()

        expectFailure<MergeFailure.CredentialsChanged> { sync.check(DB) }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
