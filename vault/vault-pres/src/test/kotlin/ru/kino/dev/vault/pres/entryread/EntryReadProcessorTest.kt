package ru.kino.dev.vault.pres.entryread

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
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation

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
    private val log = RecordingLog()

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

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryReadAction,
        initial: EntryReadState = EntryReadState(),
    ): Pair<EntryReadState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntryReadProcessor(DB, mail.value, opener, groups, navigator, log)

        processor.testWithInternalState(this, initial) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }
}
