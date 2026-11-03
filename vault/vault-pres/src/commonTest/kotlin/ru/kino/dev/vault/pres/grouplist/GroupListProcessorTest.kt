package ru.kino.dev.vault.pres.grouplist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.navigation.TrashRoute
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.FakeVaultFilterRepository
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.group.EntryCount
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupEditor
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.testing.runEach
import ru.kino.dev.vault.search.VaultSearch
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashFailure

class GroupListProcessorTest {

    @Test
    fun `an entry card gets the user name of the entry, empty when it has none`() {
        val state = GroupListState(
            listed = ListSource(
                entries = listOf(
                    EntrySummary(EntryId("mail"), GroupId("root"), "Mailbox", "alice", createdAt = 1),
                    EntrySummary(EntryId("bank"), GroupId("root"), "Bank", "", createdAt = 2),
                ),
            ),
        )

        assertEquals(mapOf("Mailbox" to "alice", "Bank" to ""), state.entries.associate { it.title to it.userName })
    }

    @Test
    fun `a database hiding user names gives the cards none`() {
        val state = GroupListState(
            listed = ListSource(
                entries = listOf(EntrySummary(EntryId("mail"), GroupId("root"), "Mailbox", "alice", createdAt = 1)),
            ),
            userNameHidden = true,
        )

        assertEquals(listOf(""), state.entries.map { it.userName })
    }

    @Test
    fun `the list reads whether the database hides user names`() = runTest {
        entrySettings.entries = entrySettings.entries.copy(protection = FieldProtection.DEFAULT.copy(userName = true))

        val (state, _) = perform(null, { onLoad() })

        assertTrue(state.userNameHidden)
    }

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

    private val entries = FakeEntryRepository()
    private val mailbox = entries.add("mailbox", groups.rootId, titled("Mailbox"))
    private val bank = entries.add("bank", groups.rootId, titled("Bank"))

    private val database = FakeVaultDatabase()
    private val filters = FakeVaultFilterRepository()
    private val log = RecordingLog()
    private val entrySettings = FakeEntrySettingsRepository()
    private val trash = FakeTrashRepository()
    private val changes = FakeVaultChanges()
    private lateinit var autoLock: AutoLock

    @Test
    fun `the root shows its groups with the entries below each, and the whole database`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() })

        assertFalse(state.loading)
        assertEquals(listOf("Passwords"), state.path.map { it.name })
        assertTrue(state.isRoot)
        assertEquals(listOf(GroupItem(work, "Work", EntryCount(2, 5)), GroupItem(home, "Home", EntryCount.None)), state.groups)
        assertEquals(EntryCount(direct = 1, nested = 6), state.entryCount)
    }

    @Test
    fun `a group shows the path down to it and its own groups`() = runTest {
        val (state, _) = perform(groupId = work.value, { onLoad() })

        assertEquals(listOf("Passwords", "Work"), state.path.map { it.name })
        assertFalse(state.isRoot)
        assertEquals(listOf(GroupItem(mail, "Mail", EntryCount(3, 3))), state.groups)
        assertEquals(5, state.entryCount.nested)
    }

    @Test
    fun `a group is known not to be the root before its tree is read`() = runTest {
        val processor = processor(groupId = work.value, navigator = recordNavigation().first)

        val state = processor.container.stateFlow.value
        assertTrue(state.loading)
        assertTrue(state.path.isEmpty())
        assertFalse(state.isRoot)
    }

    @Test
    fun `the root is known to be the root before its tree is read`() = runTest {
        val processor = processor(groupId = null, navigator = recordNavigation().first)

        assertTrue(processor.container.stateFlow.value.isRoot)
    }

    @Test
    fun `a group that is gone sends the screen back to the root`() = runTest {
        val (_, events) = perform(groupId = "gone", { onLoad() })

        assertEquals(listOf(MviNavEvent.NavigateBackTo(VaultRootRoute(DB))), events)
    }

    @Test
    fun `a tree that cannot be read says so and goes to the log`() = runTest {
        groups.failures["tree"] = GroupFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform(groupId = null, { onLoad() })

        assertFalse(state.loading)
        assertEquals(GroupListMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a tapped group opens below this one`() = runTest {
        val (_, events) = perform(groupId = null, { onLoad() }, { onOpenGroup(work) })

        assertEquals(listOf(MviNavEvent.NavigateTo(GroupRoute(DB, work.value))), events)
    }

    @Test
    fun `the database in the path goes back to the root, a group in it back to that group`() = runTest {
        val (_, events) = perform(
            groupId = mail.value,
            { onLoad() },
            { onOpenCrumb(0) },
            { onOpenCrumb(1) },
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
        val (_, events) = perform(groupId = mail.value, { onLoad() }, { onOpenCrumb(2) })

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a created group is in the list, and the file is written`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onCreateGroup() },
            { onDialogNameChange(" Bank ") },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertTrue(state.groups.any { it.name == "Bank" })
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a name a neighbour has stays in the dialog with the reason, and nothing is written`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onCreateGroup() },
            { onDialogNameChange("work") },
            { onDialogConfirm() },
        )

        assertEquals(GroupDialog.Create(name = "work", problem = NameProblem.Taken), state.dialog)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `typing again clears the reason`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onCreateGroup() },
            { onDialogConfirm() },
            { onDialogNameChange("B") },
        )

        assertEquals(GroupDialog.Create(name = "B"), state.dialog)
    }

    @Test
    fun `a renamed group shows its new name`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onRenameGroup(home) },
            { onDialogNameChange("House") },
            { onDialogConfirm() },
        )

        assertEquals("House", state.groups.single { it.id == home }.name)
        assertEquals("House", groups.name(home))
    }

    @Test
    fun `rename starts from the current name`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() }, { onRenameGroup(work) })

        assertEquals(GroupDialog.Rename(work, "Work"), state.dialog)
    }

    @Test
    fun `a deleted group leaves the list and its entries leave the count`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onDeleteGroup(work) },
            { onDialogConfirm() },
        )

        assertFalse(state.groups.any { it.id == work })
        assertEquals(1, state.entryCount.nested)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a change that is not written shows anyway, and the snackbar says it is not in the file`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onDeleteGroup(home) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(GroupListMessage.NotSaved, state.message)
        assertFalse(state.groups.any { it.id == home })
    }

    // The group shown, from the menu of the top bar

    @Test
    fun `edit in the menu of the top bar renames the group shown, from its name`() = runTest {
        val (state, _) = perform(groupId = mail.value, { onLoad() }, { onRenameCurrent() })

        assertEquals(GroupDialog.Rename(mail, "Mail"), state.dialog)
    }

    @Test
    fun `delete in the menu of the top bar asks first, naming the group shown`() = runTest {
        val (state, _) = perform(groupId = mail.value, { onLoad() }, { onDeleteCurrent() })

        assertEquals(GroupDialog.Delete(mail, "Mail", toBin = true), state.dialog)
    }

    @Test
    fun `without a recycle bin deleting a group says everything goes for good`() = runTest {
        trash.enabled = false

        val (state, _) = perform(groupId = mail.value, { onLoad() }, { onDeleteCurrent() })

        assertEquals(GroupDialog.Delete(mail, "Mail", toBin = false), state.dialog)
    }

    @Test
    fun `the root is the database - the menu of the top bar does nothing there`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() }, { onRenameCurrent() }, { onDeleteCurrent() })

        assertNull(state.dialog)
    }

    @Test
    fun `nothing to edit before the tree is read`() = runTest {
        val (state, _) = perform(groupId = mail.value, { onDeleteCurrent() })

        assertNull(state.dialog)
    }

    @Test
    fun `the group shown renamed - the path shows the new name`() = runTest {
        val (state, events) = perform(
            groupId = mail.value,
            { onLoad() },
            { onRenameCurrent() },
            { onDialogNameChange("Post") },
            { onDialogConfirm() },
        )

        assertEquals("Post", state.path.last().name)
        assertEquals("Post", groups.name(mail))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the group shown deleted - the screen goes to its parent`() = runTest {
        val (_, events) = perform(
            groupId = mail.value,
            { onLoad() },
            { onDeleteCurrent() },
            { onDialogConfirm() },
        )

        assertEquals(listOf(MviNavEvent.NavigateBackTo(GroupRoute(DB, work.value))), events)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a group of the root deleted from inside goes to the root`() = runTest {
        val (_, events) = perform(
            groupId = work.value,
            { onLoad() },
            { onDeleteCurrent() },
            { onDialogConfirm() },
        )

        assertEquals(listOf(MviNavEvent.NavigateBackTo(VaultRootRoute(DB))), events)
    }

    @Test
    fun `the group shown deleted but not written stays here, and the snackbar says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform(
            groupId = mail.value,
            { onLoad() },
            { onDeleteCurrent() },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(GroupListMessage.NotSaved, state.message)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the snackbar taking the message clears it`() = runTest {
        groups.failures["tree"] = GroupFailure.Unexpected()

        val (state, _) = perform(groupId = null, { onLoad() }, { onMessageShown() })

        assertNull(state.message)
    }

    @Test
    fun `a dialog waiting for its change does not close`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onDialogDismiss() },
            initial = GroupListState(dialog = GroupDialog.Delete(home, "Home", toBin = true, saving = true)),
        )

        assertEquals(GroupDialog.Delete(home, "Home", toBin = true, saving = true), state.dialog)
    }

    @Test
    fun `the filter button opens the dialog with the filter in effect`() = runTest {
        val kept = VaultFilter(order = CreationOrder.OldestFirst)
        filters.save(kept)
        filters.saved.clear()

        val (state, _) = perform(groupId = null, { onLoad() }, { onOpenFilters() })

        assertTrue(state.filtersOpen)
        assertEquals(kept, state.filterDraft)
    }

    @Test
    fun `the cross drops what was chosen in the dialog`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onOpenFilters() },
            { onFilterVisibilityChange(Visibility.GroupsOnly) },
            { onCloseFilters() },
        )

        assertFalse(state.filtersOpen)
        assertEquals(VaultFilter.DEFAULT, state.filter)
        assertEquals(emptyList<VaultFilter>(), filters.saved)
    }

    @Test
    fun `an applied filter takes effect and is kept on the device`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onOpenFilters() },
            { onFilterVisibilityChange(Visibility.EntriesOnly) },
            { onFilterOrderChange(CreationOrder.OldestFirst) },
            { onApplyFilters() },
        )

        val chosen = VaultFilter(Visibility.EntriesOnly, CreationOrder.OldestFirst)
        assertFalse(state.filtersOpen)
        assertEquals(chosen, state.filter)
        assertEquals(listOf(chosen), filters.saved)
        assertTrue(state.groups.isEmpty())
        assertEquals(listOf("Mailbox", "Bank"), state.entries.map { it.title })
    }

    @Test
    fun `reset puts the default in effect whatever is chosen in the dialog`() = runTest {
        filters.save(VaultFilter(visibility = Visibility.GroupsOnly))
        filters.saved.clear()

        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onOpenFilters() },
            { onFilterOrderChange(CreationOrder.OldestFirst) },
            { onResetFilters() },
        )

        assertFalse(state.filtersOpen)
        assertEquals(VaultFilter.DEFAULT, state.filter)
        assertEquals(listOf(VaultFilter.DEFAULT), filters.saved)
    }

    @Test
    fun `a filter the device does not take is in effect all the same, and the snackbar says so`() = runTest {
        filters.failSaving = true

        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onOpenFilters() },
            { onFilterVisibilityChange(Visibility.GroupsOnly) },
            { onApplyFilters() },
        )

        assertEquals(Visibility.GroupsOnly, state.filter.visibility)
        assertFalse(state.filtersOpen)
        assertFalse(state.savingFilter)
        assertEquals(GroupListMessage.FilterNotSaved, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the filter kept on the device is read when the screen is shown`() = runTest {
        filters.save(VaultFilter(order = CreationOrder.OldestFirst))

        val (state, _) = perform(groupId = null, { onLoad() })

        assertEquals(CreationOrder.OldestFirst, state.filter.order)
        assertEquals(listOf("Mailbox", "Bank"), state.entries.map { it.title })
    }

    @Test
    fun `typing searches the whole database from the root - groups by name, entries by title`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() }, { onQueryChange("ma") })

        assertTrue(state.isSearch)
        assertFalse(state.searching)
        assertEquals(listOf(GroupItem(mail, "Mail", EntryCount(3, 3), location = listOf("Passwords", "Work"))), state.groups)
        assertEquals(listOf("Mailbox"), state.entries.map { it.title })
    }

    @Test
    fun `what a search finds shows where it lies, from the group searched in`() = runTest {
        entries.add("work-mail", mail, titled("Mail of work"))

        val (state, _) = perform(groupId = work.value, { onLoad() }, { onQueryChange("mail") })

        assertEquals(listOf(listOf("Work")), state.groups.map { it.location })
        assertEquals(listOf(listOf("Work", "Mail")), state.entries.map { it.location })
    }

    @Test
    fun `the list of a group shows no location`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() })

        assertTrue(state.groups.all { it.location == null })
        assertTrue(state.entries.all { it.location == null })
    }

    @Test
    fun `in a group only what lies under it is searched`() = runTest {
        entries.add("work-mail", mail, titled("Mail of work"))

        val (state, _) = perform(groupId = work.value, { onLoad() }, { onQueryChange("MAIL") })

        assertEquals(listOf(mail), state.groups.map { it.id })
        assertEquals(listOf("Mail of work"), state.entries.map { it.title })
    }

    @Test
    fun `typing quickly searches once, for what is typed last`() = runTest {
        val (navigator, _) = recordNavigation()
        val processor = processor(groupId = null, navigator = navigator)

        processor.testWithInternalState(this, GroupListState()) {
            containerHost.runEach(arrayOf({ onLoad() }))
            containerHost.onQueryChange("b")
            containerHost.onQueryChange("ba")
            containerHost.runEach(arrayOf({ onQueryChange("ban") }))
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(1, entries.calls.count { it == "search" })
        assertEquals(listOf("Bank"), processor.container.stateFlow.value.entries.map { it.title })
    }

    @Test
    fun `a search waits for the typing to stop and shows the skeleton meanwhile`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onQueryChange("ba") },
            initial = GroupListState(loading = false, path = listOf(Crumb(groups.rootId, "Passwords"))),
            join = false,
        )

        assertTrue(state.searching)
        assertEquals(0, entries.calls.count { it == "search" })
    }

    @Test
    fun `an empty field is the group again`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onQueryChange("ma") },
            { onQueryChange("") },
        )

        assertFalse(state.isSearch)
        assertNull(state.found)
        assertEquals(listOf("Work", "Home"), state.groups.map { it.name })
        assertEquals(listOf("Bank", "Mailbox"), state.entries.map { it.title })
    }

    @Test
    fun `a search that finds nothing says so`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() }, { onQueryChange("zzz") })

        assertEquals(EmptyReason.NothingFound, state.emptyReason)
    }

    @Test
    fun `the filter applies to what the search finds`() = runTest {
        filters.save(VaultFilter(visibility = Visibility.EntriesOnly))

        val (state, _) = perform(groupId = null, { onLoad() }, { onQueryChange("ma") })

        assertTrue(state.groups.isEmpty())
        assertEquals(listOf("Mailbox"), state.entries.map { it.title })
    }

    @Test
    fun `a search that fails says so and goes to the log`() = runTest {
        entries.failures["search"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform(groupId = null, { onLoad() }, { onQueryChange("ma") })

        assertFalse(state.searching)
        assertEquals(GroupListMessage.SearchFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `going into a group clears the search`() = runTest {
        val (state, events) = perform(
            groupId = null,
            { onLoad() },
            { onQueryChange("ma") },
            { onOpenGroup(mail) },
        )

        assertEquals("", state.query)
        assertNull(state.found)
        assertEquals(listOf(MviNavEvent.NavigateTo(GroupRoute(DB, mail.value))), events)
    }

    @Test
    fun `going into an entry clears the search`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onQueryChange("ma") },
            { onOpenEntry(mailbox) },
        )

        assertEquals("", state.query)
        assertNull(state.found)
    }

    @Test
    fun `groups only in a group of entries only says there are no groups`() = runTest {
        entries.add("letter", home, titled("Letter"))
        filters.save(VaultFilter(visibility = Visibility.GroupsOnly))

        val (state, _) = perform(groupId = home.value, { onLoad() })

        assertEquals(EmptyReason.NoGroups, state.emptyReason)
    }

    @Test
    fun `entries only in a group of groups only says there are no entries`() = runTest {
        filters.save(VaultFilter(visibility = Visibility.EntriesOnly))

        val (state, _) = perform(groupId = work.value, { onLoad() })

        assertEquals(EmptyReason.NoEntries, state.emptyReason)
    }

    @Test
    fun `close fab closes the open menu`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onToggleFab() },
            { onCloseFab() },
        )

        assertFalse(state.fabExpanded)
    }

    @Test
    fun `close fab never opens the menu, a repeated one included`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onCloseFab() },
            { onCloseFab() },
        )

        assertFalse(state.fabExpanded)
    }

    @Test
    fun `the gear opens the settings`() = runTest {
        val (_, events) = perform(groupId = null, { onLoad() }, { onOpenSettings() })

        assertEquals(listOf(MviNavEvent.NavigateTo(SettingsRoute(DB))), events)
    }

    @Test
    fun `the bin beside the gear opens the recycle bin`() = runTest {
        val (_, events) = perform(groupId = null, { onLoad() }, { onOpenTrash() })

        assertEquals(listOf(MviNavEvent.NavigateTo(TrashRoute(DB))), events)
    }

    @Test
    fun `create entry opens the form inside this group and closes the menu`() = runTest {
        val (state, events) = perform(
            groupId = work.value,
            { onLoad() },
            { onToggleFab() },
            { onCreateEntry() },
        )

        assertFalse(state.fabExpanded)
        assertEquals(listOf(MviNavEvent.NavigateTo(EntryCreateRoute(DB, groupId = work.value))), events)
    }

    @Test
    fun `locking locks the database for real and leaves the locked screen to the app`() = runTest {
        val (_, events) = perform(groupId = null, { onLoad() }, { onLock() })

        assertEquals(listOf("lock"), database.calls)
        assertEquals(DB, autoLock.locked.first())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a group below the root does not hand the database to the lock`() = runTest {
        perform(groupId = work.value, { onLoad() }, { onLock() })

        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a lock that fails stays here, says so and goes to the log`() = runTest {
        database.lockFailure = CoreException("DbKeyNotFound", "not open")

        val (state, events) = perform(groupId = null, { onLoad() }, { onLock() })

        assertEquals(GroupListMessage.Unexpected, state.message)
        assertTrue(events.isEmpty())
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the entries of the group come after its groups, the newest first, untitled ones too`() = runTest {
        entries.add("blank", groups.rootId, titled(""))

        val (state, _) = perform(groupId = null, { onLoad() })

        assertEquals(listOf("", "Bank", "Mailbox"), state.entries.map { it.title })
        assertFalse(state.isEmpty)
    }

    @Test
    fun `a group with entries and no groups is not empty`() = runTest {
        entries.add("letter", home, titled("Letter"))

        val (state, _) = perform(groupId = home.value, { onLoad() })

        assertTrue(state.groups.isEmpty())
        assertFalse(state.isEmpty)
    }

    @Test
    fun `a group with neither groups nor entries is empty`() = runTest {
        val (state, _) = perform(groupId = mail.value, { onLoad() })

        assertTrue(state.isEmpty)
        assertEquals(EmptyReason.EmptyGroup, state.emptyReason)
    }

    @Test
    fun `entries that cannot be read say so and go to the log`() = runTest {
        entries.failures["entries"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform(groupId = null, { onLoad() })

        assertFalse(state.loading)
        assertEquals(GroupListMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a tapped entry opens for reading, edit from its menu opens the form`() = runTest {
        val (_, events) = perform(
            groupId = null,
            { onLoad() },
            { onOpenEntry(bank) },
            { onEditEntry(bank) },
        )

        assertEquals(
            listOf(
                MviNavEvent.NavigateTo(EntryReadRoute(DB, bank.value)),
                MviNavEvent.NavigateTo(EntryEditRoute(DB, bank.value)),
            ),
            events,
        )
    }

    @Test
    fun `delete asks first, naming the entry`() = runTest {
        val (state, _) = perform(groupId = null, { onLoad() }, { onDeleteEntry(bank) })

        assertEquals(GroupDialog.DeleteEntry(bank, "Bank", toBin = true), state.dialog)
    }

    @Test
    fun `without a recycle bin deleting an entry says it goes for good`() = runTest {
        trash.enabled = false

        val (state, _) = perform(groupId = null, { onLoad() }, { onDeleteEntry(bank) })

        assertEquals(GroupDialog.DeleteEntry(bank, "Bank", toBin = false), state.dialog)
    }

    @Test
    fun `an unread recycle bin warns of a delete for good - the safe side`() = runTest {
        trash.failures["enabled"] = TrashFailure.Unexpected()

        val (state, _) = perform(groupId = null, { onLoad() }, { onDeleteEntry(bank) })

        assertEquals(GroupDialog.DeleteEntry(bank, "Bank", toBin = false), state.dialog)
    }

    @Test
    fun `a deleted entry goes to the recycle bin, leaves the list, and the file is written`() = runTest {
        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onDeleteEntry(bank) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(listOf("Mailbox"), state.entries.map { it.title })
        assertEquals(listOf(bank), entries.recycled)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a deleted entry that is not written leaves the list anyway, and the snackbar says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onDeleteEntry(bank) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertEquals(GroupListMessage.NotSaved, state.message)
        assertFalse(state.entries.any { it.id == bank })
    }

    @Test
    fun `an entry gone before its delete closes the dialog and the list is read again`() = runTest {
        entries.failures["delete"] = EntryFailure.EntryNotFound()

        val (state, _) = perform(
            groupId = null,
            { onLoad() },
            { onDeleteEntry(bank) },
            { onDialogConfirm() },
        )

        assertNull(state.dialog)
        assertNull(state.message)
        assertEquals(2, entries.calls.count { it == "entries" })
    }

    /**
     * Runs [actions] one after another on a new processor, each to its end - [runEach]; without [join] they are
     * only started. The state they end in, and the navigation.
     */
    @Test
    fun `a change made elsewhere while the list is shown is read again`() = runTest {
        val (navigator, _) = recordNavigation()
        val processor = processor(groupId = null, navigator)

        processor.testWithInternalState(this) {
            // Listening, as while the screen is shown
            runOnCreate()
            groups.add("travel", groups.rootId, "Travel")
            changes.changed(DB)

            assertEquals(listOf("Work", "Home", "Travel"), awaitInternalState().groups.map { it.name })
            cancelAndIgnoreRemainingItems()
        }
    }

    private suspend fun TestScope.perform(
        groupId: String?,
        vararg actions: GroupListActions.() -> Unit,
        initial: GroupListState = GroupListState(isRoot = groupId == null),
        join: Boolean = true,
    ): Pair<GroupListState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = processor(groupId, navigator)

        processor.testWithInternalState(this, initial) {
            if (join) containerHost.runEach(actions) else actions.forEach { containerHost.it() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private fun TestScope.processor(groupId: String?, navigator: Navigator) = GroupListProcessor(
        DB,
        groupId,
        groups,
        GroupEditor(groups, database),
        entries,
        EntryEditor(entries, EntryOpener(entries, FakePresetRepository()), entrySettings, database),
        lock(),
        VaultSearch(groups, entries),
        filters,
        entrySettings,
        trash,
        changes,
        navigator,
        log,
    )

    /** The lock of the app, the timer never running out here - the tests lock by hand. */
    private fun TestScope.lock(): AutoLock = AutoLock(
        database = database,
        guard = DatabaseGuard(),
        timer = FakeInactivityTimer(),
        scope = backgroundScope,
        onFailure = { log.unexpected("AutoLock", it) },
    ).also { autoLock = it }

    private fun titled(title: String) = listOf(EntryField(StandardPreset.TITLE, title, protected = false))
}
