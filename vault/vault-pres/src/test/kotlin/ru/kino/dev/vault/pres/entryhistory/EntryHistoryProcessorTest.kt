package ru.kino.dev.vault.pres.entryhistory

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.history.EntryHistoryReader
import ru.kino.dev.vault.history.FakeEntryHistoryRepository
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation

class EntryHistoryProcessorTest {

    private val entries = FakeEntryRepository()
    private val history = FakeEntryHistoryRepository(entries)
    private val mail = entries.add(
        "mail",
        GroupId("root"),
        listOf(EntryField(StandardPreset.TITLE, "Mail", protected = false)),
        createdAt = 1_000L,
    )
    private val log = RecordingLog()

    @Test
    fun `the history is read when the screen is shown, newest first, with when the entry was made`() = runTest {
        entries.stored[mail] = entries.stored.getValue(mail).copy(history = listOf(1_000L, 2_000L))

        val (state, _) = perform(EntryHistoryAction.Load)

        assertFalse(state.loading)
        assertEquals("Mail", state.history?.title)
        assertEquals(listOf(2_000L, 1_000L), state.history?.versions?.map { it.modified })
        assertEquals(1_000L, state.history?.created)
    }

    @Test
    fun `before it is read there is nothing but the skeleton`() = runTest {
        val (state, _) = perform()

        assertTrue(state.loading)
        assertNull(state.history)
    }

    @Test
    fun `an entry that is gone sends the screen back`() = runTest {
        entries.stored.remove(mail)

        val (_, events) = perform(EntryHistoryAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a history that cannot be read says so and goes to the log`() = runTest {
        history.failures["versions"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, events) = perform(EntryHistoryAction.Load)

        assertFalse(state.loading)
        assertEquals(EntryHistoryMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `back goes back`() = runTest {
        val (_, events) = perform(EntryHistoryAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryHistoryAction,
    ): Pair<EntryHistoryState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntryHistoryProcessor(DB, mail.value, EntryHistoryReader(entries, history), navigator, log)

        processor.testWithInternalState(this, EntryHistoryState()) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }
}
