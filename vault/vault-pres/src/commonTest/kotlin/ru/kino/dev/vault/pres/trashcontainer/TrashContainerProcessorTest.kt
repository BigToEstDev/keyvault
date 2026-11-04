package ru.kino.dev.vault.pres.trashcontainer

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.SettingsRoute
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.navigation.TrashEntryRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.runEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrashContainerProcessorTest {

    /** What the routers answered, in order: true - taken by the container. */
    private val taken = mutableListOf<Boolean>()

    private val entries = FakeEntryRepository().apply {
        listOf("mail" to "Mail", "bank" to "Bank", "shop" to "Shop", "untitled" to "").forEach { (id, title) ->
            add(id, GroupId("bin"), listOf(EntryField(StandardPreset.TITLE, title, protected = false)))
        }
    }
    private val log = RecordingLog()

    @Test
    fun `an entry opened in the list opens its tab, shown, titled as the entry`() = runTest {
        val state = perform(open("mail"), open("untitled"))

        assertEquals(listOf(TrashTab("mail", "Mail"), TrashTab("untitled", "")), state.tabs)
        assertEquals("untitled", state.active)
        assertEquals(listOf(true, true), taken)
    }

    @Test
    fun `an entry open already is shown, not opened twice`() = runTest {
        val state = perform(open("mail"), open("bank"), open("mail"))

        assertEquals(listOf("mail", "bank"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `back in the list is the app's - back to vault`() = runTest {
        val state = perform(fromList(MviNavEvent.NavigateBack))

        assertEquals(listOf(false), taken)
        assertEquals(TrashContainerState(), state)
    }

    @Test
    fun `the rest the list goes to is the app's - the settings`() = runTest {
        perform(fromList(MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(listOf(false), taken)
    }

    @Test
    fun `back from an entry - restored, removed or gone - closes its tab, the next one shown`() = runTest {
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
    fun `back from an entry closed already is still the container's - it does not leave the bin`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") }, fromEntry("mail", MviNavEvent.NavigateBack))

        assertEquals(true, taken.last())
        assertEquals(emptyList(), state.tabs.keys)
    }

    @Test
    fun `the rest an entry goes to is the app's`() = runTest {
        perform(open("mail"), fromEntry("mail", MviNavEvent.NavigateTo(SettingsRoute(DB))))

        assertEquals(false, taken.last())
    }

    @Test
    fun `the cross of the tab shown - the one before is shown`() = runTest {
        val state = perform(open("mail"), open("bank"), { onCloseTab("bank") })

        assertEquals(listOf("mail"), state.tabs.keys)
        assertEquals("mail", state.active)
    }

    @Test
    fun `every tab closed - nothing is shown`() = runTest {
        val state = perform(open("mail"), { onCloseTab("mail") })

        assertEquals(emptyList(), state.tabs.keys)
        assertNull(state.active)
    }

    @Test
    fun `a tab selected that is not open changes nothing`() = runTest {
        val state = perform(open("mail"), { onSelectTab("bank") })

        assertEquals("mail", state.active)
    }

    @Test
    fun `the panes kept are the list and the tabs`() = runTest {
        val state = perform(open("mail"), open("bank"))

        assertEquals(setOf(TRASH_LIST_PANE, trashEntryPane("mail"), trashEntryPane("bank")), state.paneKeys)
    }

    @Test
    fun `at the most tabs an entry does not open - close one first`() = runTest {
        val state = perform(open("mail"), from = FULL)

        assertEquals(FULL.tabs, state.tabs)
        assertEquals(true, state.tabLimit)
        assertEquals(true, taken.last())
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
        assertEquals(TRASH_MAX_TABS, state.tabs.size)
    }

    private fun open(entryId: String) = fromList(MviNavEvent.NavigateTo(TrashEntryRoute(DB, entryId)))

    private fun fromList(event: MviNavEvent): TrashContainerProcessor.() -> Unit =
        { taken += listRouter.route(event) }

    private fun fromEntry(entryId: String, event: MviNavEvent): TrashContainerProcessor.() -> Unit =
        { taken += entryRouter(entryId).route(event) }

    /** Runs [actions] one after another on a new processor; the state they end in. */
    private suspend fun TestScope.perform(
        vararg actions: TrashContainerProcessor.() -> Unit,
        from: TrashContainerState = TrashContainerState(),
    ): TrashContainerState {
        val processor = TrashContainerProcessor(DB, EntryOpener(entries, FakePresetRepository()), log)
        processor.testWithInternalState(this, from) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value
    }

    private val List<TrashTab>.keys get() = map { it.entryId }

    private companion object {
        /** The most tabs open, none of them in the repository. */
        val FULL = TrashContainerState(tabs = (1..TRASH_MAX_TABS).map { TrashTab("e$it", "E$it") }, active = "e12")
    }
}
