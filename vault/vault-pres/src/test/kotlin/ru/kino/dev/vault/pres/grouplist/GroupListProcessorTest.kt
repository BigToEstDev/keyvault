package ru.kino.dev.vault.pres.grouplist

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute

class GroupListProcessorTest {

    /*
     * Passwords (1 entry)
     * ├── Work (2)
     * │   └── Mail (3)
     * └── Home (0)
     */
    private val groups = FakeGroupRepository().apply { addEntries(rootId, 1) }
    private val work = groups.add("work", groups.rootId, "Work", entries = 2)
    private val mail = groups.add("mail", work, "Mail", entries = 3)
    private val home = groups.add("home", groups.rootId, "Home")

    private val database = FakeVaultDatabase()
    private val log = RecordingLog()

    @Test
    fun `the root shows its groups with the entries below each, and the whole database`() = runTest {
        val (state, _) = perform(groupId = null, GroupListAction.Load)

        assertFalse(state.loading)
        assertEquals(listOf("Passwords"), state.path.map { it.name })
        assertTrue(state.isRoot)
        assertEquals(listOf(GroupItem(work, "Work", 5), GroupItem(home, "Home", 0)), state.groups)
        assertEquals(6, state.entryCount)
    }

    @Test
    fun `a group shows the path down to it and its own groups`() = runTest {
        val (state, _) = perform(groupId = work.value, GroupListAction.Load)

        assertEquals(listOf("Passwords", "Work"), state.path.map { it.name })
        assertFalse(state.isRoot)
        assertEquals(listOf(GroupItem(mail, "Mail", 3)), state.groups)
        assertEquals(5, state.entryCount)
    }

    @Test
    fun `a group that is gone sends the screen back to the root`() = runTest {
        val (_, events) = perform(groupId = "gone", GroupListAction.Load)

        assertEquals(listOf(MviNavEvent.NavigateBackTo(VaultRootRoute(DB))), events)
    }

    @Test
    fun `a tree that cannot be read says so and goes to the log`() = runTest {
        groups.failures["tree"] = GroupFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform(groupId = null, GroupListAction.Load)

        assertFalse(state.loading)
        assertEquals(GroupListMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a tapped group opens below this one`() = runTest {
        val (_, events) = perform(groupId = null, GroupListAction.Load, GroupListAction.OpenGroup(work))

        assertEquals(listOf(MviNavEvent.NavigateTo(GroupRoute(DB, work.value))), events)
    }

    @Test
    fun `the database in the path goes back to the root, a group in it back to that group`() = runTest {
        val (_, events) = perform(
            groupId = mail.value,
            GroupListAction.Load,
            GroupListAction.OpenCrumb(0),
            GroupListAction.OpenCrumb(1),
        )

        assertEquals(
            listOf(
                MviNavEvent.NavigateBackTo(VaultRootRoute(DB)),
                MviNavEvent.NavigateBackTo(GroupRoute(DB, work.value)),
            ),
            events,
        )
    }

    @Test
    fun `the last part of the path is where the user is, and goes nowhere`() = runTest {
        val (_, events) = perform(groupId = mail.value, GroupListAction.Load, GroupListAction.OpenCrumb(2))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a created group is in the list, and the file is written`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.CreateGroup,
            GroupListAction.DialogNameChanged(" Bank "),
            GroupListAction.DialogConfirm,
        )

        assertNull(state.dialog)
        assertTrue(state.groups.any { it.name == "Bank" })
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a name a neighbour has stays in the dialog with the reason, and nothing is written`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.CreateGroup,
            GroupListAction.DialogNameChanged("work"),
            GroupListAction.DialogConfirm,
        )

        assertEquals(GroupDialog.Create(name = "work", problem = NameProblem.Taken), state.dialog)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `typing again clears the reason`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.CreateGroup,
            GroupListAction.DialogConfirm,
            GroupListAction.DialogNameChanged("B"),
        )

        assertEquals(GroupDialog.Create(name = "B"), state.dialog)
    }

    @Test
    fun `a renamed group shows its new name`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.RenameGroup(home),
            GroupListAction.DialogNameChanged("House"),
            GroupListAction.DialogConfirm,
        )

        assertEquals("House", state.groups.single { it.id == home }.name)
        assertEquals("House", groups.name(home))
    }

    @Test
    fun `rename starts from the current name`() = runTest {
        val (state, _) = perform(groupId = null, GroupListAction.Load, GroupListAction.RenameGroup(work))

        assertEquals(GroupDialog.Rename(work, "Work"), state.dialog)
    }

    @Test
    fun `a deleted group leaves the list and its entries leave the count`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.DeleteGroup(work),
            GroupListAction.DialogConfirm,
        )

        assertFalse(state.groups.any { it.id == work })
        assertEquals(1, state.entryCount)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a change that is not written shows anyway, and the snackbar says it is not in the file`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.DeleteGroup(home),
            GroupListAction.DialogConfirm,
        )

        assertNull(state.dialog)
        assertEquals(GroupListMessage.NotSaved, state.message)
        assertFalse(state.groups.any { it.id == home })
    }

    @Test
    fun `the snackbar taking the message clears it`() = runTest {
        groups.failures["tree"] = GroupFailure.Unexpected()

        val (state, _) = perform(groupId = null, GroupListAction.Load, GroupListAction.MessageShown)

        assertNull(state.message)
    }

    @Test
    fun `a dialog waiting for its change does not close`() = runTest {
        val (state, _) = perform(
            groupId = null,
            GroupListAction.DialogDismiss,
            initial = GroupListState(dialog = GroupDialog.Delete(home, "Home", saving = true)),
        )

        assertEquals(GroupDialog.Delete(home, "Home", saving = true), state.dialog)
    }

    @Test
    fun `the filter button opens the dialog, and it closes`() = runTest {
        val (opened, _) = perform(groupId = null, GroupListAction.Load, GroupListAction.OpenFilters)
        val (closed, _) = perform(
            groupId = null,
            GroupListAction.Load,
            GroupListAction.OpenFilters,
            GroupListAction.CloseFilters,
        )

        assertTrue(opened.filtersOpen)
        assertFalse(closed.filtersOpen)
    }

    @Test
    fun `create entry opens its stub inside this group and closes the menu`() = runTest {
        val (state, events) = perform(
            groupId = work.value,
            GroupListAction.Load,
            GroupListAction.ToggleFab,
            GroupListAction.CreateEntry,
        )

        assertFalse(state.fabExpanded)
        assertEquals(listOf(MviNavEvent.NavigateTo(EntryEditRoute(DB, parentGroupId = work.value))), events)
    }

    @Test
    fun `locking locks the database for real and puts the locked screen on top`() = runTest {
        val (state, events) = perform(groupId = null, GroupListAction.Load, GroupListAction.Lock)

        assertFalse(state.locking)
        assertEquals(listOf("lock"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateTo(LockedRoute(DB))), events)
    }

    @Test
    fun `a lock that fails stays here, says so and goes to the log`() = runTest {
        database.lockFailure = CoreException("DbKeyNotFound", "not open")

        val (state, events) = perform(groupId = null, GroupListAction.Load, GroupListAction.Lock)

        assertEquals(GroupListMessage.Unexpected, state.message)
        assertTrue(events.isEmpty())
        assertEquals(1, log.logged.size)
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        groupId: String?,
        vararg actions: GroupListAction,
        initial: GroupListState = GroupListState(),
    ): Pair<GroupListState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = GroupListProcessor(DB, groupId, groups, GroupEditor(groups, database), database, navigator, log)

        processor.testWithInternalState(this, initial) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }
        return navigator to events
    }

    private class FakeVaultDatabase : VaultDatabaseRepository {
        val calls = mutableListOf<String>()
        var saveFailure: Exception? = null
        var lockFailure: CoreException? = null

        override suspend fun save(dbKey: String) {
            calls += "save"
            saveFailure?.let { throw it }
        }

        override suspend fun lock(dbKey: String) {
            calls += "lock"
            lockFailure?.let { throw it }
        }

        override suspend fun close(dbKey: String) = error("not here")
        override suspend fun hasChangedElsewhere(dbKey: String): Boolean = error("not here")
        override suspend fun merge(dbKey: String): MergeSummary = error("not here")
    }

    private class RecordingLog : ErrorLog {
        val logged = mutableListOf<Throwable>()

        override fun unexpected(where: String, error: Throwable) {
            logged += error
        }
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
