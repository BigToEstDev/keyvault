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
    fun `back from the form of a new entry - saved or left - closes its tab`() = runTest {
        val state = perform(open("mail"), create("work"), fromForm(newTab(1), MviNavEvent.NavigateBack))

        assertEquals(listOf("mail"), state.tabs.keys)
        assertEquals("mail", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `edit in an entry read turns its tab to the form, in its place`() = runTest {
        val state = perform(open("mail"), open("bank"), fromEntry("mail", MviNavEvent.NavigateTo(EntryEditRoute(DB, "mail"))))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", editing = true), EntryTab.Entry("bank", "Bank")), state.tabs)
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
    fun `an entry deleted on its form closes its tab`() = runTest {
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
        assertEquals(listOf(EntryTab.Entry("mail", "Mail", editing = true)), state.tabs)
    }

    @Test
    fun `back of the reading of an entry on its form already leaves the tab as it is`() = runTest {
        val state = perform(open("mail"), edit("mail"), fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(listOf(EntryTab.Entry("mail", "Mail", editing = true)), state.tabs)
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
    fun `the cross of a form not shown shows it, the tab stays until the form goes back`() = runTest {
        val state = perform(open("mail"), edit("mail"), open("bank"), { onCloseTab("mail") })

        assertEquals(listOf("mail", "bank"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `the cross of the form of an entry is the back of the form`() = runTest {
        val editing = VaultContainerState(tabs = listOf(EntryTab.Entry("mail", "Mail", editing = true)), active = "mail")

        processor().testWithInternalState(this, editing) {
            containerHost.onCloseTab("mail")

            expectSideEffect(VaultContainerSideEffect.PaneBack(formPane("mail")))
        }
    }

    @Test
    fun `the cross of a new entry is the back of its form`() = runTest {
        val making = VaultContainerState(tabs = listOf(EntryTab.New(newTab(1), "work")), active = newTab(1))

        processor().testWithInternalState(this, making) {
            containerHost.onCloseTab(newTab(1))

            expectSideEffect(VaultContainerSideEffect.PaneBack(newTab(1)))
        }
    }

    @Test
    fun `back from an entry closed already is still the container's - it does not leave vault`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") }, fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(true, taken.last())
        assertEquals(emptyList(), state.tabs.keys)
    }

    @Test
    fun `the rest an entry goes to is the app's - its history`() = runTest {
        val state = perform(open("mail"), fromEntry("mail", MviNavEvent.NavigateTo(EntryHistoryRoute(DB, "mail"))))

        assertEquals(listOf(true, false), taken)
        assertEquals(listOf("mail"), state.tabs.keys)
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

    private fun open(entryId: String) = fromList(MviNavEvent.NavigateTo(EntryReadRoute(DB, entryId)))

    private fun group(groupId: String) = fromList(MviNavEvent.NavigateTo(GroupRoute(DB, groupId)))

    private fun create(groupId: String) = fromList(MviNavEvent.NavigateTo(EntryCreateRoute(DB, groupId)))

    private fun edit(entryId: String) = fromEntry(entryId, MviNavEvent.NavigateTo(EntryEditRoute(DB, entryId)))

    private fun fromList(event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromEntry(entryId: String, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += readRouter(entryId).route(event) }

    private fun fromForm(key: String, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += formRouter(key).route(event) }

    /** Runs [actions] one after another on a new processor; the state they end in. */
    private suspend fun TestScope.perform(vararg actions: VaultContainerProcessor.() -> Unit): VaultContainerState {
        val processor = processor()
        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value
    }

    private fun processor() = VaultContainerProcessor(DB, EntryOpener(entries, FakePresetRepository()), changes, log)

    private val List<EntryTab>.keys get() = map { it.key }
}
