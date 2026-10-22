package ru.kino.dev.vault.pres.trash

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure

class TrashProcessorTest {

    private val trash = FakeTrashRepository().apply {
        entries += binned("old", "Old mail", deletedAt = 100)
        entries += binned("new", "Bank", deletedAt = 300)
        entries += binned("same-b", "beta", deletedAt = 200)
        entries += binned("same-a", "Alpha", deletedAt = 200)
    }
    private val log = RecordingLog()

    @Test
    fun `the bin comes the last deleted first, one second by title`() = runTest {
        val (state, _) = perform(TrashAction.Load)

        assertEquals(listOf("new", "same-a", "same-b", "old"), state.shown.map { it.entry.id.value })
        assertNull(state.empty)
    }

    @Test
    fun `an empty bin says so`() = runTest {
        trash.entries.clear()

        val (state, _) = perform(TrashAction.Load)

        assertEquals(TrashEmpty.Bin, state.empty)
    }

    @Test
    fun `a bin that cannot be read says so and goes to the log`() = runTest {
        trash.failures["entries"] = TrashFailure.Unexpected()

        val (state, _) = perform(TrashAction.Load)

        assertEquals(TrashMessage.LoadFailed, state.message)
        assertFalse(state.loading)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `typing searches the bin by title`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.QueryChanged("MAIL"))

        assertFalse(state.searching)
        assertEquals(listOf("old"), state.shown.map { it.entry.id.value })
    }

    @Test
    fun `a search that finds nothing says so`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.QueryChanged("nothing"))

        assertEquals(TrashEmpty.NothingFound, state.empty)
    }

    @Test
    fun `a blank field is the whole bin again`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.QueryChanged("mail"), TrashAction.QueryChanged(" "))

        assertNull(state.found)
        assertEquals(4, state.shown.size)
    }

    @Test
    fun `a failed search says so`() = runTest {
        trash.failures["search"] = TrashFailure.Unexpected()

        val (state, _) = perform(TrashAction.Load, TrashAction.QueryChanged("mail"))

        assertFalse(state.searching)
        assertEquals(TrashMessage.SearchFailed, state.message)
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(TrashAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(vararg actions: TrashAction): Pair<TrashState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = TrashProcessor(DB, trash, navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun binned(id: String, title: String, deletedAt: Long) = TrashEntry(
        entry = EntrySummary(EntryId(id), GroupId("bin"), title, "", createdAt = 0),
        previousGroupId = null,
        deletedAt = deletedAt,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
