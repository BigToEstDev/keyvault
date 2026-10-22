package ru.kino.dev.vault.pres.entryread

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.trash.FakeTrashRepository

class EntryReadProcessorTest {

    private val entries = FakeEntryRepository()
    private val opener = EntryOpener(entries, FakePresetRepository())
    private val mail = entries.add(
        "mail",
        GroupId("root"),
        listOf(
            EntryField(StandardPreset.TITLE, "Mail", protected = false),
            EntryField(StandardPreset.PASSWORD, "secret", protected = true),
            EntryField("PIN", "1234", protected = true),
        ),
    )
    private val groups = FakeGroupRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val settings = FakeEntrySettingsRepository()
    private val trash = FakeTrashRepository()

    @Test
    fun `the group of the entry is shown with the path down to it`() = runTest {
        val (state, _) = perform(EntryReadAction.Load)

        assertEquals(GroupChoice(groups.rootId, listOf("Passwords")), state.group)
    }

    @Test
    fun `an entry moved on its form comes back in its new group`() = runTest {
        val shown = EntryReadState(
            loading = false,
            content = opener.open(DB, mail).content,
            group = GroupChoice(groups.rootId, listOf("Passwords")),
        )
        val work = groups.add("work", groups.rootId, "Work")
        entries.stored[mail] = entries.stored.getValue(mail).copy(groupId = work)

        val (state, _) = perform(EntryReadAction.Load, initial = shown)

        assertEquals(GroupChoice(work, listOf("Passwords", "Work")), state.group)
    }

    @Test
    fun `the entry is read when the screen is shown, laid over the preset, everything hidden`() = runTest {
        val (state, _) = perform(EntryReadAction.Load)

        assertFalse(state.loading)
        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertEquals(listOf("PIN"), state.content?.ownFields?.map { it.key })
        assertFalse(state.passwordVisible)
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `the fields private by the database come with the entry, and the eye shows one`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = perform(EntryReadAction.Load, EntryReadAction.ToggleKey(StandardPreset.TITLE))

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
        assertEquals(setOf(StandardPreset.TITLE), state.shownKeys)
    }

    @Test
    fun `the private fields changed in the settings - the entry comes back hidden again`() = runTest {
        val shown = EntryReadState(
            loading = false,
            protection = FieldProtection.DEFAULT,
            shownKeys = setOf(StandardPreset.TITLE),
        )
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = perform(EntryReadAction.Load, initial = shown)

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
        assertTrue(state.shownKeys.isEmpty())
    }

    @Test
    fun `what is shown stays shown when the entry comes back the same`() = runTest {
        val (state, _) = perform(
            EntryReadAction.Load,
            EntryReadAction.TogglePassword,
            EntryReadAction.ToggleOwn(0),
            EntryReadAction.Load,
        )

        assertTrue(state.passwordVisible)
        assertEquals(setOf(0), state.shownOwn)
    }

    @Test
    fun `an entry changed on its form comes back read anew and hidden again`() = runTest {
        val shown = EntryReadState(
            loading = false,
            content = opener.open(DB, mail).content,
            passwordVisible = true,
            shownOwn = setOf(0),
        )
        entries.stored[mail] = entries.stored.getValue(mail).let { entry ->
            entry.copy(fields = entry.fields.map { if (it.key == StandardPreset.TITLE) it.copy(value = "Post") else it })
        }

        val (state, _) = perform(EntryReadAction.Load, initial = shown)

        assertEquals("Post", state.content?.value(StandardPreset.TITLE))
        assertFalse(state.passwordVisible)
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `an entry gone closes the screen`() = runTest {
        entries.stored.remove(mail)

        val (_, events) = perform(EntryReadAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an entry that cannot be read says so and goes to the log`() = runTest {
        entries.failures["read"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform(EntryReadAction.Load, EntryReadAction.Edit)

        assertFalse(state.loading)
        assertNull(state.content)
        assertEquals(EntryReadMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the pen opens the form of this entry, back goes back`() = runTest {
        val (_, events) = perform(EntryReadAction.Load, EntryReadAction.Edit, EntryReadAction.Back)

        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntryEditRoute(DB, mail.value)), MviNavEvent.NavigateBack),
            events,
        )
    }

    @Test
    fun `the entry is read with when it was made and last changed`() = runTest {
        entries.stored[mail] = entries.stored.getValue(mail).copy(createdAt = 1_000L, modifiedAt = 2_000L)

        val (state, _) = perform(EntryReadAction.Load)

        assertEquals(EntryTimes(created = 1_000L, modified = 2_000L), state.times)
    }

    @Test
    fun `back from its form, the entry shows the new time of its change`() = runTest {
        val read = opener.open(DB, mail)
        val shown = EntryReadState(
            loading = false,
            content = read.content,
            group = GroupChoice(groups.rootId, listOf("Passwords")),
            times = read.times,
        )
        entries.stored[mail] = entries.stored.getValue(mail).copy(modifiedAt = 9_000L)

        val (state, _) = perform(EntryReadAction.Load, initial = shown)

        assertEquals(9_000L, state.times.modified)
    }

    @Test
    fun `history in the menu opens the history of this entry`() = runTest {
        val (_, events) = perform(EntryReadAction.Load, EntryReadAction.History)

        assertEquals(listOf(MviNavEvent.NavigateTo(EntryHistoryRoute(DB, mail.value))), events)
    }

    @Test
    fun `no history before the entry is read`() = runTest {
        val (_, events) = perform(EntryReadAction.History)

        assertTrue(events.isEmpty())
    }

    // Delete, from the menu of the top bar

    @Test
    fun `delete asks first, naming the entry`() = runTest {
        val (state, _) = perform(EntryReadAction.Load, EntryReadAction.Delete)

        assertEquals(DeleteDialog("Mail", toBin = true), state.delete)
    }

    @Test
    fun `without a recycle bin the dialog says the delete is for good`() = runTest {
        trash.enabled = false

        val (state, _) = perform(EntryReadAction.Load, EntryReadAction.Delete)

        assertEquals(DeleteDialog("Mail", toBin = false), state.delete)
    }

    @Test
    fun `nothing to delete before the entry is read`() = runTest {
        val (state, _) = perform(EntryReadAction.Delete)

        assertNull(state.delete)
    }

    @Test
    fun `a deleted entry goes to the recycle bin, the file is written and the screen goes back`() = runTest {
        val (_, events) = perform(EntryReadAction.Load, EntryReadAction.Delete, EntryReadAction.DeleteConfirm)

        assertEquals(listOf(mail), entries.recycled)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a deleted entry that is not written stays here, and the snackbar says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform(EntryReadAction.Load, EntryReadAction.Delete, EntryReadAction.DeleteConfirm)

        assertNull(state.delete)
        assertEquals(EntryReadMessage.NotSaved, state.message)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel closes the dialog, the entry stays`() = runTest {
        val (state, _) = perform(EntryReadAction.Load, EntryReadAction.Delete, EntryReadAction.DeleteDismiss)

        assertNull(state.delete)
        assertTrue(entries.recycled.isEmpty())
    }

    @Test
    fun `an entry in the recycle bin is not shown - the screen goes back`() = runTest {
        // Deleted from its form over this screen: back here, it is in the bin - in a group put there
        val old = groups.add("old", groups.rootId, "Old")
        groups.delete(DB, old)
        entries.stored[mail] = entries.stored.getValue(mail).copy(groupId = old)

        val (_, events) = perform(EntryReadAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the lock button locks the database for real and leaves the locked screen to the app`() = runTest {
        val lock = lock()
        lock.watch(DB)

        val (state, events) = perform(EntryReadAction.Lock, autoLock = lock)

        assertEquals(listOf("lock"), database.calls)
        assertEquals(DB, lock.locked.first())
        assertTrue(state.locking)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a lock the core refuses says so and lets the button be tapped again`() = runTest {
        database.lockFailure = CoreException("DbKeyNotFound", "not open")
        val lock = lock()
        lock.watch(DB)

        val (state, _) = perform(EntryReadAction.Lock, autoLock = lock)

        assertFalse(state.locking)
        assertEquals(EntryReadMessage.Unexpected, state.message)
    }

    /** The lock of the app, the timer never running out here - the tests lock by hand. */
    private fun TestScope.lock(): AutoLock = AutoLock(
        database = database,
        guard = DatabaseGuard(),
        timer = FakeInactivityTimer(),
        scope = backgroundScope,
        onFailure = { log.unexpected("AutoLock", it) },
    )

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryReadAction,
        initial: EntryReadState = EntryReadState(),
        autoLock: AutoLock = lock(),
    ): Pair<EntryReadState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val editor = EntryEditor(entries, opener, settings, database)
        val processor = EntryReadProcessor(DB, mail.value, opener, settings, groups, editor, trash, autoLock, navigator, log)

        processor.testWithInternalState(this, initial) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }
}
