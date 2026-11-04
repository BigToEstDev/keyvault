package ru.kino.dev.vault.pres.container

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.navigation.EntryCreateRoute
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.pres.navigation.EntryVersionRoute
import ru.kino.dev.vault.pres.navigation.GroupRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.runEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultContainerProcessorTest {

    /** What the routers answered, in order: true - taken by the container. */
    private val taken = mutableListOf<Boolean>()

    private val entries = FakeEntryRepository().apply {
        listOf("mail" to "Mail", "bank" to "Bank", "shop" to "Shop", "untitled" to "").forEach { (id, title) ->
            add(id, GroupId("root"), listOf(EntryField(StandardPreset.TITLE, title, protected = false)))
        }
    }
    private val changes = FakeVaultChanges()
    private val log = RecordingLog()

    @Test
    fun `a tab is titled as its entry`() = runTest {
        val state = perform(open("mail"), open("untitled"))

        assertEquals(listOf("Mail", ""), state.tabs.map { (it as EntryTab.Entry).title })
    }

    @Test
    fun `an entry renamed elsewhere renames its tab`() = runTest {
        val processor = processor()

        val shown = VaultContainerState(tabs = listOf(EntryTab.Entry("mail", "Mail")), active = "mail")

        processor.testWithInternalState(this, shown) {
            // Listening, as while the container is shown
            runOnCreate()
            val stored = entries.stored.getValue(EntryId("mail"))
            entries.stored[EntryId("mail")] = stored.copy(fields = listOf(EntryField(StandardPreset.TITLE, "Mail, renamed", false)))
            changes.changed(DB)

            assertEquals(listOf("Mail, renamed"), awaitInternalState().tabs.map { (it as EntryTab.Entry).title })
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `an entry opened in the list opens its tab, shown`() = runTest {
        val state = perform(open("mail"), open("bank"))

        assertEquals(listOf("mail", "bank"), state.tabs.keys)
        assertEquals("bank", state.active)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `an entry open already is shown, not opened twice`() = runTest {
        val state = perform(open("mail"), open("bank"), open("mail"))

        assertEquals(listOf("mail", "bank"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `a group opened in the list is shown on the left, the way down kept`() = runTest {
        val state = perform(group("work"), group("mail"))

        assertEquals(listOf(null, "work", "mail"), state.groups)
        assertEquals("mail", state.group)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `back in the list goes one group up`() = runTest {
        val state = perform(group("work"), group("mail"), fromList(MviNavEvent.NavigateBack))

        assertEquals(listOf(null, "work"), state.groups)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back in the list at the root is the app's, as on a phone`() = runTest {
        val state = perform(fromList(MviNavEvent.NavigateBack))

        assertEquals(listOf<String?>(null), state.groups)
        assertEquals(listOf(false), taken)
    }

    @Test
    fun `a crumb goes back to its group`() = runTest {
        val state = perform(group("work"), group("mail"), fromList(MviNavEvent.NavigateBackTo(GroupRoute(DB, "work"))))

        assertEquals(listOf(null, "work"), state.groups)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the first crumb goes back to the root`() = runTest {
        val state = perform(group("work"), group("mail"), fromList(MviNavEvent.NavigateBackTo(VaultRootRoute(DB))))

        assertEquals(listOf<String?>(null), state.groups)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the rest the list goes to is the app's - the settings`() = runTest {
        val state = perform(fromList(MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(listOf(false), taken)
        assertEquals(VaultContainerState(), state)
    }

    @Test
    fun `a new entry made in the list opens a tab of its own, shown - the tab open stays`() = runTest {
        val state = perform(open("mail"), create("work"))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail"), EntryTab.New(newTab(1), "work")), state.tabs)
        assertEquals(newTab(1), state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `each new entry made has a tab of its own`() = runTest {
        val state = perform(create("work"), create("work"))

        assertEquals(listOf(newTab(1), newTab(2)), state.tabs.keys)
    }

    @Test
    fun `a new entry saved - its tab reads it, in the same place`() = runTest {
        val read = MviNavEvent.NavigateTo(EntryReadRoute(DB, "bank"), popUpTo = EntryCreateRoute(DB, "work"), inclusive = true)

        val state = perform(create("work"), open("mail"), { onSelectTab(newTab(1)) }, fromNew(newTab(1), read))

        assertEquals(listOf(EntryTab.Entry("bank", "Bank"), EntryTab.Entry("mail", "Mail")), state.tabs)
        assertEquals("bank", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `a new entry left closes its tab`() = runTest {
        val state = perform(open("mail"), create("work"), fromNew(newTab(1), MviNavEvent.NavigateBack))

        assertEquals(listOf("mail"), state.tabs.keys)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in an entry read turns its tab to the form, in its place`() = runTest {
        val state = perform(open("mail"), open("bank"), fromEntry("mail", MviNavEvent.NavigateTo(EntryEditRoute(DB, "mail"))))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit)), EntryTab.Entry("bank", "Bank")), state.tabs)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in the list of an entry not open opens its tab on the form, shown`() = runTest {
        val state = perform(open("bank"), editFromList("mail"))

        assertEquals(listOf(EntryTab.Entry("bank", "Bank"), EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit))), state.tabs)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in the list of an entry read turns its tab to the form, shown`() = runTest {
        val state = perform(open("mail"), open("bank"), editFromList("mail"))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit)), EntryTab.Entry("bank", "Bank")), state.tabs)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in the list of an entry on its history opens the form over it`() = runTest {
        val state = perform(open("mail"), history("mail"), editFromList("mail"))

        assertEquals(listOf(EntryPage.History, EntryPage.Edit), (state.tabs.single() as EntryTab.Entry).pages)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in the list of an entry on its form already only shows it`() = runTest {
        val state = perform(open("mail"), edit("mail"), open("bank"), editFromList("mail"))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit)), EntryTab.Entry("bank", "Bank")), state.tabs)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from the form opened in the list reads the entry, its tab stays`() = runTest {
        val state = perform(editFromList("mail"), fromForm("mail", MviNavEvent.NavigateBack))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail")), state.tabs)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from the form of an entry - saved or left - reads it again`() = runTest {
        val state = perform(open("mail"), edit("mail"), fromForm("mail", MviNavEvent.NavigateBack))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail")), state.tabs)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `an entry deleted on its form, or the form closed, closes its tab`() = runTest {
        val deleted = MviNavEvent.NavigateBackTo(EntryReadRoute(DB, "mail"), inclusive = true, orBack = true)

        val state = perform(open("mail"), open("bank"), { onSelectTab("mail") }, edit("mail"), fromForm("mail", deleted))

        assertEquals(listOf("bank"), state.tabs.keys)
        assertEquals("bank", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the rest a form goes to is the app's - a new preset`() = runTest {
        val state = perform(open("mail"), edit("mail"), fromForm("mail", MviNavEvent.NavigateTo(PresetEditRoute())))

        assertEquals(false, taken.last())
        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit))), state.tabs)
    }

    @Test
    fun `back of the reading of an entry on its form already leaves the tab as it is`() = runTest {
        val state = perform(open("mail"), edit("mail"), fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit))), state.tabs)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from an entry closes its tab, the next one shown`() = runTest {
        val state = perform(
            open("mail"),
            open("bank"),
            open("shop"),
            { onSelectTab("bank") },
            fromEntry("bank", MviNavEvent.NavigateBack),
        )

        assertEquals(listOf("mail", "shop"), state.tabs.keys)
        assertEquals("shop", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the last tab closed - the one before is shown`() = runTest {
        val state = perform(open("mail"), open("bank"), { onCloseTab("bank") })

        assertEquals(listOf("mail"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `a tab not shown closed - the shown one stays`() = runTest {
        val state = perform(open("mail"), open("bank"), { onCloseTab("mail") })

        assertEquals(listOf("bank"), state.tabs.keys)
        assertEquals("bank", state.active)
    }

    @Test
    fun `every tab closed - nothing is shown`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") })

        assertEquals(emptyList(), state.tabs.keys)
        assertNull(state.active)
    }

    @Test
    fun `the cross of a form not shown shows it, the tab stays until the form goes away`() = runTest {
        val state = perform(open("mail"), edit("mail"), open("bank"), { onCloseTab("mail") })

        assertEquals(listOf("mail", "bank"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `the cross of the form of an entry closes the form - it asks`() = runTest {
        val editing = VaultContainerState(tabs = listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.Edit))), active = "mail")

        processor().testWithInternalState(this, editing) {
            containerHost.onCloseTab("mail")

            expectSideEffect(VaultContainerSideEffect.ClosePane(formPane("mail")))
        }
    }

    @Test
    fun `the cross of a new entry closes its form - it asks`() = runTest {
        val making = VaultContainerState(tabs = listOf(EntryTab.New(newTab(1), "work")), active = newTab(1))

        processor().testWithInternalState(this, making) {
            containerHost.onCloseTab(newTab(1))

            expectSideEffect(VaultContainerSideEffect.ClosePane(newTab(1)))
        }
    }

    @Test
    fun `back from an entry closed already is still the container's - it does not leave vault`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") }, fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(true, taken.last())
        assertEquals(emptyList(), state.tabs.keys)
    }

    @Test
    fun `history in an entry read opens over its reading, in its tab`() = runTest {
        val state = perform(open("mail"), history("mail"))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", pages = listOf(EntryPage.History))), state.tabs)
        assertEquals(true, taken.last())
    }

    @Test
    fun `a version opened in the history opens over it`() = runTest {
        val state = perform(open("mail"), history("mail"), version("mail", 2))

        assertEquals(listOf(EntryPage.History, EntryPage.Version(2)), (state.tabs.single() as EntryTab.Entry).pages)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back from a version - the history`() = runTest {
        val state = perform(
            open("mail"),
            history("mail"),
            version("mail", 2),
            fromPage("mail", EntryPage.Version(2), MviNavEvent.NavigateBack),
        )

        assertEquals(listOf(EntryPage.History), (state.tabs.single() as EntryTab.Entry).pages)
    }

    @Test
    fun `back from the history - the entry read`() = runTest {
        val state = perform(open("mail"), history("mail"), fromPage("mail", EntryPage.History, MviNavEvent.NavigateBack))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail")), state.tabs)
        assertEquals(true, taken.last())
    }

    @Test
    fun `a version restored - the entry read, past the history`() = runTest {
        val restored = MviNavEvent.NavigateBackTo(EntryReadRoute(DB, "mail"), orBack = true)

        val state = perform(open("mail"), history("mail"), version("mail", 2), fromPage("mail", EntryPage.Version(2), restored))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail")), state.tabs)
        assertEquals(true, taken.last())
    }

    @Test
    fun `back of a page not shown any more leaves the tab as it is`() = runTest {
        val state = perform(
            open("mail"),
            history("mail"),
            version("mail", 2),
            fromPage("mail", EntryPage.History, MviNavEvent.NavigateBack),
        )

        assertEquals(listOf(EntryPage.History, EntryPage.Version(2)), (state.tabs.single() as EntryTab.Entry).pages)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the cross of a version closes the whole tab`() = runTest {
        val state = perform(open("mail"), open("bank"), history("mail"), version("mail", 2), { onCloseTab("mail") })

        assertEquals(listOf("bank"), state.tabs.keys)
    }

    @Test
    fun `the cross of the history closes the whole tab`() = runTest {
        val state = perform(open("mail"), history("mail"), { onCloseTab("mail") })

        assertEquals(emptyList(), state.tabs.keys)
    }

    @Test
    fun `the rest a page of an entry goes to is the app's - the settings`() = runTest {
        perform(open("mail"), history("mail"), fromPage("mail", EntryPage.History, MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(false, taken.last())
    }

    @Test
    fun `an entry on its history keeps its reading too`() = runTest {
        val state = perform(open("mail"), history("mail"), version("mail", 2))

        assertEquals(
            setOf(groupPane(null), entryPane("mail"), historyPane("mail"), versionPane("mail", 2)),
            state.paneKeys,
        )
    }

    @Test
    fun `a tab selected that is not open changes nothing`() = runTest {
        val state = perform(open("mail"), { onSelectTab("bank") })

        assertEquals("mail", state.active)
    }

    @Test
    fun `back of the system goes one group up`() = runTest {
        val state = perform(group("work"), group("mail"), { onGroupUp() })

        assertEquals(listOf(null, "work"), state.groups)
    }

    @Test
    fun `back of the system at the root stays at the root`() = runTest {
        val state = perform({ onGroupUp() })

        assertEquals(listOf<String?>(null), state.groups)
    }

    @Test
    fun `the panes kept are the groups on the way down and the tabs`() = runTest {
        val state = perform(group("work"), open("mail"), create("work"))

        assertEquals(setOf(groupPane(null), groupPane("work"), entryPane("mail"), newTab(1)), state.paneKeys)
    }

    @Test
    fun `an entry on its form keeps its reading too`() = runTest {
        val state = perform(open("mail"), edit("mail"))

        assertEquals(setOf(groupPane(null), entryPane("mail"), formPane("mail")), state.paneKeys)
    }

    @Test
    fun `at the most tabs an entry read does not open - close one first`() = runTest {
        val state = perform(open("mail"), from = FULL)

        assertEquals(FULL.tabs, state.tabs)
        assertEquals(true, state.tabLimit)
        assertEquals(true, taken.last())
    }

    @Test
    fun `at the most tabs edit from the list and a new entry do not open either`() = runTest {
        val state = perform(editFromList("mail"), create("root"), from = FULL)

        assertEquals(FULL.tabs, state.tabs)
        assertEquals(true, state.tabLimit)
    }

    @Test
    fun `at the most tabs an entry open already is shown, nothing asked`() = runTest {
        val state = perform(open("e1"), from = FULL)

        assertEquals("e1", state.active)
        assertEquals(false, state.tabLimit)
    }

    @Test
    fun `close one first taken - it goes, a tab closed makes room`() = runTest {
        val state = perform(open("mail"), { onTabLimitDismiss() }, { onCloseTab("e1") }, open("mail"), from = FULL)

        assertEquals(false, state.tabLimit)
        assertEquals("mail", state.active)
        assertEquals(VAULT_MAX_TABS, state.tabs.size)
    }

    private fun open(entryId: String) = fromList(MviNavEvent.NavigateTo(EntryReadRoute(DB, entryId)))

    private fun group(groupId: String) = fromList(MviNavEvent.NavigateTo(GroupRoute(DB, groupId)))

    private fun create(groupId: String) = fromList(MviNavEvent.NavigateTo(EntryCreateRoute(DB, groupId)))

    private fun edit(entryId: String) = fromEntry(entryId, MviNavEvent.NavigateTo(EntryEditRoute(DB, entryId)))

    private fun editFromList(entryId: String) = fromList(MviNavEvent.NavigateTo(EntryEditRoute(DB, entryId)))

    private fun fromList(event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromEntry(entryId: String, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += entryRouter(entryId, page = null).route(event) }

    private fun fromForm(entryId: String, event: MviNavEvent) = fromPage(entryId, EntryPage.Edit, event)

    private fun fromPage(entryId: String, page: EntryPage, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += entryRouter(entryId, page).route(event) }

    private fun fromNew(key: String, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += newRouter(key).route(event) }

    private fun history(entryId: String) = fromEntry(entryId, MviNavEvent.NavigateTo(EntryHistoryRoute(DB, entryId)))

    private fun version(entryId: String, index: Int) =
        fromPage(entryId, EntryPage.History, MviNavEvent.NavigateTo(EntryVersionRoute(DB, entryId, index)))

    /** Runs [actions] one after another on a new processor; the state they end in. */
    private suspend fun TestScope.perform(
        vararg actions: VaultContainerProcessor.() -> Unit,
        from: VaultContainerState = VaultContainerState(),
    ): VaultContainerState {
        val processor = processor()
        processor.testWithInternalState(this, from) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value
    }

    private fun processor() = VaultContainerProcessor(DB, EntryOpener(entries, FakePresetRepository()), changes, log)

    private val List<EntryTab>.keys get() = map { it.key }

    private companion object {
        /** The most tabs open, none of them in the repository: read, nothing to ask. */
        val FULL = VaultContainerState(tabs = (1..VAULT_MAX_TABS).map { EntryTab.Entry("e$it", "E$it") }, active = "e12")
    }
}
