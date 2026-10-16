package ru.kino.dev.vault.pres.entryversion

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.history.EntryHistoryEditor
import ru.kino.dev.vault.history.EntryVersionOpener
import ru.kino.dev.vault.history.FakeEntryHistoryRepository
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation

class EntryVersionProcessorTest {

    private val entries = FakeEntryRepository()
    private val history = FakeEntryHistoryRepository(entries)
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val mail = entries.add(
        "mail",
        GroupId("root"),
        listOf(title("Mail"), EntryField("PIN", "1234", protected = true)),
        createdAt = 1_000L,
    )

    init {
        entries.stored[mail] = entries.stored.getValue(mail).updatedTo(listOf(title("Post")), now = 2_000L)
    }

    @Test
    fun `the version is read when the screen is shown, as it was, everything hidden`() = runTest {
        val (state, _) = perform(EntryVersionAction.Load)

        assertFalse(state.loading)
        assertEquals("Mail", state.version?.content?.value(StandardPreset.TITLE))
        assertEquals(1_000L, state.version?.times?.modified)
        assertFalse(state.passwordVisible)
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `the eye shows a protected field of the version`() = runTest {
        val (state, _) = perform(EntryVersionAction.Load, EntryVersionAction.ToggleOwn(0))

        assertEquals(setOf(0), state.shownOwn)
    }

    @Test
    fun `restore asks first`() = runTest {
        val (state, _) = perform(EntryVersionAction.Load, EntryVersionAction.Restore)

        assertEquals(false, state.restoring)
    }

    @Test
    fun `a version restored is the entry again, and the screen goes to its reading, past the history`() = runTest {
        val (_, events) = perform(
            EntryVersionAction.Load,
            EntryVersionAction.Restore,
            EntryVersionAction.DialogConfirm,
        )

        assertEquals("Mail", entries.stored.getValue(mail).fields.first().value)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBackTo(EntryReadRoute(DB, mail.value), orBack = true)), events)
    }

    @Test
    fun `a restore not written stays here and says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform(
            EntryVersionAction.Load,
            EntryVersionAction.Restore,
            EntryVersionAction.DialogConfirm,
        )

        assertNull(state.restoring)
        assertEquals(EntryVersionMessage.NotSaved, state.message)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel closes the dialog, nothing restored`() = runTest {
        val (state, _) = perform(EntryVersionAction.Load, EntryVersionAction.Restore, EntryVersionAction.DialogDismiss)

        assertNull(state.restoring)
        assertEquals("Post", entries.stored.getValue(mail).fields.first().value)
    }

    @Test
    fun `a version that is gone sends the screen back to the history`() = runTest {
        entries.stored[mail] = entries.stored.getValue(mail).copy(history = emptyList())

        val (_, events) = perform(EntryVersionAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `nothing to restore before the version is read`() = runTest {
        val (state, _) = perform(EntryVersionAction.Restore)

        assertNull(state.restoring)
    }

    private fun title(value: String) = EntryField(StandardPreset.TITLE, value, protected = false)

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryVersionAction,
    ): Pair<EntryVersionState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntryVersionProcessor(
            DB,
            mail.value,
            index = 0,
            EntryVersionOpener(history, EntryOpener(entries, FakePresetRepository())),
            EntryHistoryEditor(history, database),
            navigator,
            log,
        )

        processor.testWithInternalState(this, EntryVersionState()) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }
}
