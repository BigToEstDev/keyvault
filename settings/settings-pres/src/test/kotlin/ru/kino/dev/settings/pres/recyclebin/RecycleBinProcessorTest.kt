package ru.kino.dev.settings.pres.recyclebin

import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure

class RecycleBinProcessorTest {

    private val trash = FakeTrashRepository().apply {
        entries += binned("mail")
        entries += binned("bank")
    }
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the switch and what the bin holds`() = runTest {
        val (state, _) = perform(RecycleBinAction.Load)

        assertEquals(true, state.enabled)
        assertEquals(2, state.count)
    }

    @Test
    fun `a bin that cannot be read says so and goes to the log`() = runTest {
        trash.failures["enabled"] = TrashFailure.Unexpected()

        val (state, _) = perform(RecycleBinAction.Load)

        assertNull(state.enabled)
        assertEquals(RecycleBinMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `switching off a bin that holds something asks first and writes nothing`() = runTest {
        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false))

        assertTrue(state.confirmingOff)
        assertEquals(true, state.enabled)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `switched off after asking, the bin goes and the file is written`() = runTest {
        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false), RecycleBinAction.OffConfirm)

        assertEquals(false, state.enabled)
        assertEquals(0, state.count)
        assertFalse(state.confirmingOff)
        assertFalse(state.saving)
        assertFalse(trash.enabled)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `cancelled, the bin stays as it was`() = runTest {
        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false), RecycleBinAction.OffDismiss)

        assertEquals(true, state.enabled)
        assertEquals(2, state.count)
        assertTrue(trash.enabled)
    }

    @Test
    fun `an empty bin is switched off without asking`() = runTest {
        trash.entries.clear()

        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false))

        assertFalse(state.confirmingOff)
        assertEquals(false, state.enabled)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `switched on, it is written at once`() = runTest {
        trash.enabled = false
        trash.entries.clear()

        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(true))

        assertEquals(true, state.enabled)
        assertTrue(trash.enabled)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a file not written keeps the switch and says so`() = runTest {
        trash.entries.clear()
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false))

        assertEquals(false, state.enabled)
        assertEquals(RecycleBinMessage.NotSaved, state.message)
    }

    @Test
    fun `a switch the core refused goes back`() = runTest {
        trash.entries.clear()
        trash.failures["setEnabled"] = TrashFailure.Unexpected()

        val (state, _) = perform(RecycleBinAction.Load, RecycleBinAction.SetEnabled(false))

        assertEquals(true, state.enabled)
        assertFalse(state.saving)
        assertEquals(RecycleBinMessage.Failed, state.message)
    }

    @Test
    fun `deleted opens the bin`() = runTest {
        val (_, events) = perform(RecycleBinAction.Load, RecycleBinAction.OpenTrash)

        assertEquals(listOf(MviNavEvent.NavigateTo(TrashRoute(DB))), events)
    }

    @Test
    fun `an empty bin opens nothing`() = runTest {
        trash.entries.clear()

        val (_, events) = perform(RecycleBinAction.Load, RecycleBinAction.OpenTrash)

        assertEquals(emptyList<MviNavEvent>(), events)
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(RecycleBinAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(vararg actions: RecycleBinAction): Pair<RecycleBinState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = RecycleBinProcessor(DB, trash, TrashEditor(trash, database), navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun binned(id: String) = TrashEntry(
        entry = EntrySummary(EntryId(id), GroupId("bin"), id, "", createdAt = 0),
        previousGroupId = null,
        deletedAt = 0,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
