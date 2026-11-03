package ru.kino.dev.vault.pres.container

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
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

        assertEquals(listOf("Mail", ""), state.tabs.map { it.title })
    }

    @Test
    fun `an entry renamed elsewhere renames its tab`() = runTest {
        val processor = processor()

        val shown = VaultContainerState(tabs = listOf(EntryTab("mail", "Mail")), active = "mail")

        processor.testWithInternalState(this, shown) {
            // Listening, as while the container is shown
            runOnCreate()
            val stored = entries.stored.getValue(EntryId("mail"))
            entries.stored[EntryId("mail")] = stored.copy(fields = listOf(EntryField(StandardPreset.TITLE, "Mail, renamed", false)))
            changes.changed(DB)

            assertEquals(listOf("Mail, renamed"), awaitInternalState().tabs.map { it.title })
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `an entry opened in the list opens its tab, shown`() = runTest {
        val state = perform(open("mail"), open("bank"))

        assertEquals(listOf("mail", "bank"), state.tabs.ids)
        assertEquals("bank", state.active)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `an entry open already is shown, not opened twice`() = runTest {
        val state = perform(open("mail"), open("bank"), open("mail"))

        assertEquals(listOf("mail", "bank"), state.tabs.ids)
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
    fun `the rest the list goes to is the app's - the settings, the form of an entry`() = runTest {
        val state = perform(
            fromList(MviNavEvent.NavigateTo(SettingsRoute(DB))),
            fromList(MviNavEvent.NavigateTo(EntryEditRoute(DB, "mail"))),
        )

        assertEquals(listOf(false, false), taken)
        assertEquals(VaultContainerState(), state)
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

        assertEquals(listOf("mail", "shop"), state.tabs.ids)
        assertEquals("shop", state.active)
        assertEquals(true, taken.last())
    }

    @Test
    fun `the last tab closed - the one before is shown`() = runTest {
        val state = perform(open("mail"), open("bank"), { onCloseTab("bank") })

        assertEquals(listOf("mail"), state.tabs.ids)
        assertEquals("mail", state.active)
    }

    @Test
    fun `a tab not shown closed - the shown one stays`() = runTest {
        val state = perform(open("mail"), open("bank"), { onCloseTab("mail") })

        assertEquals(listOf("bank"), state.tabs.ids)
        assertEquals("bank", state.active)
    }

    @Test
    fun `every tab closed - nothing is shown`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") })

        assertEquals(emptyList(), state.tabs.ids)
        assertNull(state.active)
    }

    @Test
    fun `back from an entry closed already is still the container's - it does not leave vault`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") }, fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(true, taken.last())
        assertEquals(emptyList(), state.tabs.ids)
    }

    @Test
    fun `the rest an entry goes to is the app's - its history, its form`() = runTest {
        val state = perform(
            open("mail"),
            fromEntry("mail", MviNavEvent.NavigateTo(EntryHistoryRoute(DB, "mail"))),
            fromEntry("mail", MviNavEvent.NavigateTo(EntryEditRoute(DB, "mail"))),
        )

        assertEquals(listOf(true, false, false), taken)
        assertEquals(listOf("mail"), state.tabs.ids)
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
        val state = perform(group("work"), open("mail"))

        assertEquals(setOf(groupPane(null), groupPane("work"), entryPane("mail")), state.paneKeys)
    }

    private fun open(entryId: String) = fromList(MviNavEvent.NavigateTo(EntryReadRoute(DB, entryId)))

    private fun group(groupId: String) = fromList(MviNavEvent.NavigateTo(GroupRoute(DB, groupId)))

    private fun fromList(event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromEntry(entryId: String, event: MviNavEvent): VaultContainerProcessor.() -> Unit =
        { taken += entryRouter(entryId).route(event) }

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

    private val List<EntryTab>.ids get() = map { it.entryId }
}
