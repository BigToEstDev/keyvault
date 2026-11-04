package ru.kino.dev.vault.pres.entryhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryHistoryReader
import ru.kino.dev.vault.history.FakeEntryHistoryRepository
import ru.kino.dev.vault.pres.navigation.EntryVersionRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.testing.runEach

class EntryHistoryProcessorTest {

    private val entries = FakeEntryRepository()
    private val history = FakeEntryHistoryRepository(entries)
    private val mail = entries.add(
        "mail",
        GroupId("root"),
        listOf(EntryField(StandardPreset.TITLE, "Mail", protected = false)),
        createdAt = 1_000L,
    )
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val groups = FakeGroupRepository()
    private val changes = FakeVaultChanges()

    @Test
    fun `the history is read when the screen is shown, newest first, with when the entry was made`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (state, _) = perform({ onLoad() })

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

        val (_, events) = perform({ onLoad() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a history that cannot be read says so and goes to the log`() = runTest {
        history.failures["versions"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, events) = perform({ onLoad() })

        assertFalse(state.loading)
        assertEquals(EntryHistoryMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `back goes back`() = runTest {
        val (_, events) = perform({ onBack() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    // A version, from the menu of its card

    @Test
    fun `a tap on a version opens it`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (_, events) = perform({ onLoad() }, { onOpenVersion(1) })

        assertEquals(listOf(MviNavEvent.NavigateTo(EntryVersionRoute(DB, mail.value, 1))), events)
    }

    @Test
    fun `restore asks first, naming when the version was saved`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (state, _) = perform({ onLoad() }, { onRestore(0) })

        assertEquals(EntryHistoryDialog.Restore(0, 1_000L), state.dialog)
    }

    @Test
    fun `a version restored is the entry again, and the screen goes back to it`() = runTest {
        entries.update(DB, mail, listOf(EntryField(StandardPreset.TITLE, "Post", protected = false)))

        val (_, events) = perform(
            { onLoad() },
            { onRestore(0) },
            { onDialogConfirm() },
        )

        assertEquals("Mail", entries.stored.getValue(mail).fields.single().value)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `delete asks first, naming when the version was saved`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (state, _) = perform({ onLoad() }, { onDeleteVersion(1) })

        assertEquals(EntryHistoryDialog.Delete(1, 2_000L), state.dialog)
    }

    @Test
    fun `a version deleted leaves the history read again, the screen stays`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (state, events) = perform(
            { onLoad() },
            { onDeleteVersion(1) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(listOf(1_000L), state.history?.versions?.map { it.modified })
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a change not written says so here, with the history read again`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform(
            { onLoad() },
            { onDeleteVersion(1) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(EntryHistoryMessage.NotSaved, state.message)
        assertEquals(listOf(1_000L), state.history?.versions?.map { it.modified })
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel closes the dialog, the history stays`() = runTest {
        entries.putHistory(mail, 1_000L)

        val (state, _) = perform(
            { onLoad() },
            { onDeleteVersion(0) },
            { onDialogDismiss() },
        )

        assertNull(state.dialog)
        assertEquals(1, entries.stored.getValue(mail).history.size)
    }

    @Test
    fun `a change under way does not close`() = runTest {
        val (state, _) = perform(
            { onDialogDismiss() },
            initial = EntryHistoryState(dialog = EntryHistoryDialog.Delete(0, 1_000L, saving = true)),
        )

        assertEquals(EntryHistoryDialog.Delete(0, 1_000L, saving = true), state.dialog)
    }

    // The whole history, from the menu of the top bar

    @Test
    fun `delete history asks first`() = runTest {
        entries.putHistory(mail, 1_000L)

        val (state, _) = perform({ onLoad() }, { onDeleteHistory() })

        assertEquals(EntryHistoryDialog.DeleteAll(), state.dialog)
    }

    @Test
    fun `no history to delete - no dialog`() = runTest {
        val (state, _) = perform({ onLoad() }, { onDeleteHistory() })

        assertNull(state.dialog)
    }

    @Test
    fun `the whole history deleted, the screen goes back to the entry`() = runTest {
        entries.putHistory(mail, 1_000L, 2_000L)

        val (_, events) = perform(
            { onLoad() },
            { onDeleteHistory() },
            { onDialogConfirm() },
        )

        assertTrue(entries.stored.getValue(mail).history.isEmpty())
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the whole history deleted but not written stays here and says so`() = runTest {
        entries.putHistory(mail, 1_000L)
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform(
            { onLoad() },
            { onDeleteHistory() },
            { onDialogConfirm() },
        )

        assertEquals(EntryHistoryMessage.NotSaved, state.message)
        assertEquals(emptyList(), state.history?.versions)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `no dialog for a version the history shown does not have`() = runTest {
        val (state, _) = perform({ onLoad() }, { onRestore(7) })

        assertNull(state.dialog)
    }

    @Test
    fun `an entry in the recycle bin - put there beside the screen - has no history to show, back`() = runTest {
        entries.stored[mail] = entries.stored.getValue(mail).copy(groupId = GroupId("bin"))

        val (state, events) = perform({ onLoad() })

        assertNull(state.history)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a change made elsewhere while the history is shown is read again`() = runTest {
        entries.putHistory(mail, 1_000L)
        val (navigator, _) = recordNavigation()

        processor(navigator).testWithInternalState(this) {
            // Listening, as while the screen is shown
            runOnCreate()
            entries.putHistory(mail, 1_000L, 2_000L)
            changes.changed(DB)

            assertEquals(listOf(2_000L, 1_000L), awaitInternalState().history?.versions?.map { it.modified })
            cancelAndIgnoreRemainingItems()
        }
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryHistoryActions.() -> Unit,
        initial: EntryHistoryState = EntryHistoryState(),
    ): Pair<EntryHistoryState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = processor(navigator)

        processor.testWithInternalState(this, initial) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun processor(navigator: Navigator) = EntryHistoryProcessor(
        DB,
        mail.value,
        EntryHistoryReader(entries, history),
        EntryHistoryEditor(history, database),
        groups,
        changes,
        navigator,
        log,
    )
}
