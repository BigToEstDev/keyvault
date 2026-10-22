package ru.kino.dev.vault.pres.trash

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
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.navigation.TrashEntryRoute
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashEditor
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
    private val database = FakeVaultDatabase()
    private val groups = FakeGroupRepository()
    private val work = groups.add("work", groups.rootId, "Work")
    private val home = groups.add("home", groups.rootId, "Home")

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

    // Choosing

    @Test
    fun `a tap with nothing picked reads the entry`() = runTest {
        val (state, events) = perform(TrashAction.Load, TrashAction.Tap(OLD))

        assertFalse(state.choosing)
        assertEquals(listOf(MviNavEvent.NavigateTo(TrashEntryRoute(DB, OLD.value))), events)
    }

    @Test
    fun `a long press starts the choice, a tap then picks and lets go`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Tap(NEW), TrashAction.Tap(OLD))

        assertEquals(setOf(NEW), state.picked)
    }

    @Test
    fun `pick all takes every card shown`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.PickAll)

        assertEquals(4, state.picked.size)
    }

    @Test
    fun `back lets go of the choice and stays`() = runTest {
        val (state, events) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Back)

        assertFalse(state.choosing)
        assertTrue(events.isEmpty())
    }

    // Restore

    @Test
    fun `restore offers the group the picked came from when they share it`() = runTest {
        trash.entries.clear()
        trash.entries += binned("a", "A", 1, previous = work)
        trash.entries += binned("b", "B", 2, previous = work)

        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(EntryId("a")), TrashAction.Tap(EntryId("b")), TrashAction.Restore)

        val dialog = state.dialog as TrashDialog.Restore
        assertEquals(work, dialog.group)
        assertEquals(listOf(groups.rootId, work, home), dialog.groups.map { it.id })
    }

    @Test
    fun `restore offers no group when the picked came from different ones`() = runTest {
        trash.entries.clear()
        trash.entries += binned("a", "A", 1, previous = work)
        trash.entries += binned("b", "B", 2, previous = home)

        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(EntryId("a")), TrashAction.Tap(EntryId("b")), TrashAction.Restore)

        assertNull((state.dialog as TrashDialog.Restore).group)
    }

    @Test
    fun `restore without a group chosen does nothing`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Restore, TrashAction.DialogConfirm)

        assertTrue(state.dialog is TrashDialog.Restore)
        assertTrue(trash.restored.isEmpty())
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `restored to the group chosen, the bin is read again and the choice ends`() = runTest {
        val (state, _) = perform(
            TrashAction.Load,
            TrashAction.LongPress(OLD),
            TrashAction.Restore,
            TrashAction.GroupChosen(home),
            TrashAction.DialogConfirm,
        )

        assertEquals(mapOf(OLD to home), trash.restored)
        assertEquals(listOf("save"), database.calls)
        assertNull(state.dialog)
        assertFalse(state.choosing)
        assertEquals(3, state.shown.size)
    }

    // Remove and empty

    @Test
    fun `the picked removed for good after asking`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Tap(NEW), TrashAction.Remove, TrashAction.DialogConfirm)

        assertEquals(listOf("same-b", "same-a"), trash.entries.map { it.entry.id.value }.sortedDescending())
        assertEquals(listOf("save"), database.calls)
        assertEquals(2, state.shown.size)
    }

    @Test
    fun `remove asks with the number picked`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Tap(NEW), TrashAction.Remove)

        assertEquals(TrashDialog.Remove(2), state.dialog)
    }

    @Test
    fun `emptying after asking takes the whole bin`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.Empty, TrashAction.DialogConfirm)

        assertEquals(TrashEmpty.Bin, state.empty)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `cancelled, nothing changes`() = runTest {
        val (state, _) = perform(TrashAction.Load, TrashAction.Empty, TrashAction.DialogDismiss)

        assertNull(state.dialog)
        assertEquals(4, state.shown.size)
    }

    @Test
    fun `a change not written says so, and it is made`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(TrashAction.Load, TrashAction.Empty, TrashAction.DialogConfirm)

        assertEquals(TrashMessage.NotSaved, state.message)
        assertEquals(TrashEmpty.Bin, state.empty)
    }

    @Test
    fun `a stale list says the bin changed, and it is read again`() = runTest {
        trash.failures["remove"] = TrashFailure.Stale()

        val (state, _) = perform(TrashAction.Load, TrashAction.LongPress(OLD), TrashAction.Remove, TrashAction.DialogConfirm)

        assertEquals(TrashMessage.Changed, state.message)
        assertFalse(state.choosing)
        assertEquals(4, state.shown.size)
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(TrashAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(vararg actions: TrashAction): Pair<TrashState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = TrashProcessor(DB, trash, TrashEditor(trash, database), groups, navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun binned(id: String, title: String, deletedAt: Long, previous: GroupId? = null) = TrashEntry(
        entry = EntrySummary(EntryId(id), GroupId("bin"), title, "", createdAt = 0),
        previousGroupId = previous,
        deletedAt = deletedAt,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        val OLD = EntryId("old")
        val NEW = EntryId("new")
    }
}
