package ru.kino.dev.vault.pres.trashentry

import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.trash.TrashDialog
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure

class TrashEntryProcessorTest {

    private val groups = FakeGroupRepository()
    private val work = groups.add("work", groups.rootId, "Work")
    private val entries = FakeEntryRepository()
    private val mail = entries.add(
        "mail",
        BIN,
        listOf(
            EntryField(StandardPreset.TITLE, "Mail", protected = false),
            EntryField(StandardPreset.PASSWORD, "secret", protected = true),
        ),
    )
    private val trash = FakeTrashRepository().apply {
        entries += TrashEntry(EntrySummary(mail, BIN, "Mail", "", createdAt = 0), previousGroupId = work, deletedAt = 500)
    }
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()

    @Test
    fun `the deleted entry is read with when it was deleted`() = runTest {
        val (state, _) = perform(TrashEntryAction.Load)

        assertEquals("Mail", state.entry?.content?.value(StandardPreset.TITLE))
        assertEquals(500L, state.trashed?.deletedAt)
        assertFalse(state.loading)
    }

    @Test
    fun `an entry no longer in the bin sends the screen back`() = runTest {
        trash.entries.clear()

        val (state, events) = perform(TrashEntryAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertNull(state.entry)
    }

    @Test
    fun `a bin that cannot be read says so and goes to the log`() = runTest {
        trash.failures["entries"] = TrashFailure.Unexpected()

        val (state, _) = perform(TrashEntryAction.Load)

        assertEquals(TrashEntryMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `restore offers the group the entry came from`() = runTest {
        val (state, _) = perform(TrashEntryAction.Load, TrashEntryAction.Restore)

        assertEquals(work, (state.dialog as TrashDialog.Restore).group)
    }

    @Test
    fun `restored, the file is written and the screen goes back to the bin`() = runTest {
        val (_, events) = perform(TrashEntryAction.Load, TrashEntryAction.Restore, TrashEntryAction.DialogConfirm)

        assertEquals(mapOf(mail to work), trash.restored)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `removed for good after asking, back to the bin`() = runTest {
        val (state, events) = perform(TrashEntryAction.Load, TrashEntryAction.Remove, TrashEntryAction.DialogConfirm)

        assertTrue(trash.entries.isEmpty())
        assertEquals(TrashDialog.Remove(1, saving = true), state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `cancelled, nothing changes`() = runTest {
        val (state, events) = perform(TrashEntryAction.Load, TrashEntryAction.Remove, TrashEntryAction.DialogDismiss)

        assertNull(state.dialog)
        assertEquals(1, trash.entries.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a file not written is said, and OK goes back`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, events) = perform(
            TrashEntryAction.Load,
            TrashEntryAction.Remove,
            TrashEntryAction.DialogConfirm,
            TrashEntryAction.NotSavedSeen,
        )

        assertTrue(state.notSaved)
        assertNull(state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an entry gone from the bin meanwhile sends the screen back`() = runTest {
        trash.failures["remove"] = TrashFailure.Stale()

        val (_, events) = perform(TrashEntryAction.Load, TrashEntryAction.Remove, TrashEntryAction.DialogConfirm)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the eye shows the password`() = runTest {
        val (state, _) = perform(TrashEntryAction.Load, TrashEntryAction.TogglePassword)

        assertTrue(state.passwordVisible)
    }

    private suspend fun TestScope.perform(vararg actions: TrashEntryAction): Pair<TrashEntryState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = TrashEntryProcessor(
            DB,
            mail.value,
            EntryOpener(entries, FakePresetRepository()),
            FakeEntrySettingsRepository(),
            trash,
            TrashEditor(trash, database),
            groups,
            navigator,
            log,
        )

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        val BIN = GroupId("bin")
    }
}
