package ru.kino.dev.vault.pres.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.runEach
import ru.kino.dev.vault.sync.AppReturns
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.SaveStatus
import ru.kino.dev.vault.sync.SyncRequests
import ru.kino.dev.vault.sync.VaultSync

class SyncProcessorTest {

    private val database = FakeVaultDatabase()
    private val status = SaveStatus()
    private val requests = SyncRequests()
    private val returns = AppReturns()
    private val log = RecordingLog()

    private val nothing = database.mergeSummary.copy(addedEntries = 0)

    @Test
    fun `coming up, a file written elsewhere is merged quietly and what came in is said`() = runTest {
        database.changedElsewhere = true

        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            assertTrue(awaitInternalState().syncing)
            val done = awaitInternalState()

            assertEquals(SyncDialog.Done(database.mergeSummary), done.dialog)
            assertEquals(false, done.syncing)
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(listOf("hasChangedElsewhere", "merge", "save"), database.calls)
    }

    @Test
    fun `coming up, a file that is ours shows nothing`() = runTest {
        val processor = processor()
        processor.testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            cancelAndIgnoreRemainingItems()
        }
        val state = processor.container.stateFlow.value

        assertNull(state.dialog)
        assertEquals(listOf("hasChangedElsewhere"), database.calls)
    }

    @Test
    fun `a merge that brought nothing shows nothing`() = runTest {
        database.changedElsewhere = true
        database.mergeSummary = nothing

        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            assertTrue(awaitInternalState().syncing)
            assertEquals(SyncState(), awaitInternalState())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a save that wrote nothing asks Synchronize or Cancel`() = runTest {
        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            status.changedElsewhere(DB)

            assertEquals(SyncDialog.Ask, awaitInternalState().dialog)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a save of another database asks nothing here`() = runTest {
        val processor = processor()
        processor.testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            status.changedElsewhere("content://docs/other.kdbx")
            cancelAndIgnoreRemainingItems()
        }

        assertNull(processor.container.stateFlow.value.dialog)
    }

    @Test
    fun `Synchronize merges, saves and says what came in`() = runTest {
        val state = perform({ onSynchronize() }, initial = SyncState(dialog = SyncDialog.Ask))

        assertEquals(SyncDialog.Done(database.mergeSummary), state.dialog)
        assertEquals(listOf("merge", "save"), database.calls)
    }

    @Test
    fun `Cancel closes the question, the change stays in memory`() = runTest {
        val state = perform({ onCancel() }, initial = SyncState(dialog = SyncDialog.Ask))

        assertNull(state.dialog)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a file saved elsewhere with other credentials is said, nothing saved over it`() = runTest {
        database.mergeFailure = MergeFailure.CredentialsChanged()

        val state = perform({ onSynchronize() }, initial = SyncState(dialog = SyncDialog.Ask))

        assertEquals(SyncDialog.Failed(SyncProblem.CredentialsChanged), state.dialog)
        assertEquals(listOf("merge"), database.calls)
    }

    @Test
    fun `a merge not written is said`() = runTest {
        database.saveFailure = DatabaseFileException("disk full")

        val state = perform({ onSynchronize() }, initial = SyncState(dialog = SyncDialog.Ask))

        assertEquals(SyncDialog.Failed(SyncProblem.NotSaved), state.dialog)
    }

    @Test
    fun `OK closes what was said`() = runTest {
        val state = perform({ onDismiss() }, initial = SyncState(dialog = SyncDialog.Failed(SyncProblem.Damaged)))

        assertNull(state.dialog)
    }

    @Test
    fun `the bar of the main screen asks for a sync - merged and saved at once`() = runTest {
        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            requests.synchronize(DB)

            assertTrue(awaitInternalState().syncing)
            assertEquals(SyncDialog.Done(database.mergeSummary), awaitInternalState().dialog)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failure of the quiet check is said once, not on every coming back`() = runTest {
        database.changedElsewhere = true
        database.mergeFailure = MergeFailure.CredentialsChanged()

        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            assertTrue(awaitInternalState().syncing)
            assertEquals(SyncDialog.Failed(SyncProblem.CredentialsChanged), awaitInternalState().dialog)
            containerHost.onDismiss()
            assertNull(awaitInternalState().dialog)

            returns.cameBack()
            assertTrue(awaitInternalState().syncing)
            assertEquals(SyncState(), awaitInternalState())
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(2, database.calls.count { it == "merge" })
    }

    @Test
    fun `a write the file did not hold asks Save or Later`() = runTest {
        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            status.damaged(DB)

            assertEquals(SyncDialog.Damaged, awaitInternalState().dialog)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a damaged write of another database asks nothing here`() = runTest {
        val processor = processor()
        processor.testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            status.damaged("content://docs/other.kdbx")
            cancelAndIgnoreRemainingItems()
        }

        assertNull(processor.container.stateFlow.value.dialog)
    }

    @Test
    fun `Save writes again and closes the question`() = runTest {
        status.damaged(DB)
        database.onSave = { status.saved(DB) }

        val state = perform({ onSave() }, initial = SyncState(dialog = SyncDialog.Damaged))

        assertEquals(SyncState(), state)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `Save that fails again asks again`() = runTest {
        status.damaged(DB)
        database.saveFailure = DatabaseFileException("offline")

        val state = perform({ onSave() }, initial = SyncState(dialog = SyncDialog.Damaged))

        assertEquals(SyncState(dialog = SyncDialog.Damaged), state)
    }

    @Test
    fun `Later closes the question, the change stays in memory`() = runTest {
        val state = perform({ onLater() }, initial = SyncState(dialog = SyncDialog.Damaged))

        assertNull(state.dialog)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `the bar of the main screen asks to save - written again at once`() = runTest {
        status.damaged(DB)
        database.onSave = { status.saved(DB) }

        processor().testWithInternalState(this) {
            runOnCreate()
            // Up to its listening: the signals come to nobody before it
            testScheduler.runCurrent()
            requests.save(DB)

            assertTrue(awaitInternalState().saving)
            assertEquals(SyncState(), awaitInternalState())
            cancelAndIgnoreRemainingItems()
        }
    }

    private fun processor() = SyncProcessor(DB, VaultSync(database, status, DatabaseGuard()), status, requests, returns, log)

    private suspend fun TestScope.perform(vararg actions: SyncActions.() -> Unit, initial: SyncState): SyncState =
        processor().let { processor ->
            processor.testWithInternalState(this, initial) {
                containerHost.runEach(actions)
                cancelAndIgnoreRemainingItems()
            }
            processor.container.stateFlow.value
        }
}
