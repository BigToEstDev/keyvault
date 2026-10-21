package ru.kino.dev.settings.pres.entries

import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.appearance.FakeAppearanceRepository
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entries.EntrySettingsEditor
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository

class EntriesProcessorTest {

    private val settings = FakeEntrySettingsRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private var appearance = FakeAppearanceRepository(initialUserNameInList = true)

    @Test
    fun `the screen shown reads the settings`() = runTest {
        val (state, _) = perform(EntriesAction.Load)

        assertEquals(settings.entries, state.entries)
        assertEquals(10, state.keptItems)
        assertEquals(SIX_MB, state.keptSize)
    }

    @Test
    fun `settings that cannot be read say so and go to the log`() = runTest {
        settings.failure = EntrySettingsFailure.Unexpected()

        val (state, _) = perform(EntriesAction.Load)

        assertNull(state.entries)
        assertEquals(EntriesMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a hidden field switched on is written at once`() = runTest {
        val protection = FieldProtection.DEFAULT.copy(title = true)

        val (state, _) = perform(EntriesAction.Load, EntriesAction.SetProtection(protection))

        assertEquals(protection, state.entries?.protection)
        assertEquals(protection, settings.entries.protection)
        assertNull(state.saving)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the user name made private takes it out of the list`() = runTest {
        perform(EntriesAction.Load, EntriesAction.SetProtection(FieldProtection.DEFAULT.copy(userName = true)))

        assertEquals(listOf(false), appearance.savedUserNameInList)
    }

    @Test
    fun `the user name made private but not written takes it out of the list too`() = runTest {
        database.saveFailure = IOException("provider gone")

        perform(EntriesAction.Load, EntriesAction.SetProtection(FieldProtection.DEFAULT.copy(userName = true)))

        assertEquals(listOf(false), appearance.savedUserNameInList)
    }

    @Test
    fun `the user name refused by the core leaves the list as it was`() = runTest {
        perform(EntriesAction.Load, FailNext(settings), EntriesAction.SetProtection(FieldProtection.DEFAULT.copy(userName = true)))

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `another field made private leaves the list as it was`() = runTest {
        perform(EntriesAction.Load, EntriesAction.SetProtection(FieldProtection.DEFAULT.copy(url = true)))

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `the list not showing user names is not written again`() = runTest {
        appearance = FakeAppearanceRepository(initialUserNameInList = false)

        perform(EntriesAction.Load, EntriesAction.SetProtection(FieldProtection.DEFAULT.copy(userName = true)))

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `a higher limit is written without asking`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.SelectItems(20))

        assertNull(state.trimming)
        assertEquals(HistoryLimits(20, SIX_MB), settings.entries.history)
    }

    @Test
    fun `a lower limit asks first, nothing changed meanwhile`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.SelectItems(5))

        assertEquals(TrimDialog(HistoryLimits(5, SIX_MB), EntriesPart.Items), state.trimming)
        assertEquals(HistoryLimits(10, SIX_MB), settings.entries.history)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `trim confirmed keeps the lower limit and writes the file`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.SelectSize(2 * MEGABYTE), EntriesAction.TrimConfirm)

        assertNull(state.trimming)
        assertEquals(HistoryLimits(10, 2 * MEGABYTE), state.entries?.history)
        assertEquals(HistoryLimits(10, 2 * MEGABYTE), settings.entries.history)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `trim dismissed keeps the limit as it was`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.SelectItems(5), EntriesAction.TrimDismiss)

        assertNull(state.trimming)
        assertEquals(HistoryLimits(10, SIX_MB), state.entries?.history)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `no limit is written without asking`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.LimitItems(false))

        assertEquals(HistoryLimits(null, SIX_MB), settings.entries.history)
        assertEquals(10, state.keptItems)
    }

    @Test
    fun `a limit switched on where there was none takes the default and asks first`() = runTest {
        settings.entries = settings.entries.copy(history = HistoryLimits(null, null))

        val (state, _) = perform(EntriesAction.Load, EntriesAction.LimitSize(true))

        assertEquals(TrimDialog(HistoryLimits(null, 10 * MEGABYTE), EntriesPart.Size), state.trimming)
    }

    @Test
    fun `the change made but not written stays shown and says so`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(EntriesAction.Load, EntriesAction.SelectItems(20))

        assertEquals(HistoryLimits(20, SIX_MB), state.entries?.history)
        assertEquals(EntriesMessage.NotSaved, state.message)
        assertNull(state.saving)
    }

    @Test
    fun `a change the core refused goes back and says so`() = runTest {
        val protection = FieldProtection.DEFAULT.copy(notes = true)

        val (state, _) = perform(
            EntriesAction.Load,
            FailNext(settings),
            EntriesAction.SetProtection(protection),
        )

        assertEquals(FieldProtection.DEFAULT, state.entries?.protection)
        assertEquals(EntriesMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the wheel opens on the value tapped`() = runTest {
        val (state, _) = perform(EntriesAction.Load, EntriesAction.Choose(HistoryLimit.Size))

        assertEquals(HistoryLimit.Size, state.choosing)
    }

    @Test
    fun `a size of the file not on the wheel is offered in its place`() {
        assertEquals(
            listOf(2, 5, 6, 10, 15, 20, 50).map { it * MEGABYTE },
            HistoryChoices.sizes(SIX_MB),
        )
        assertEquals(51, HistoryChoices.items(10).size)
    }

    /** Runs [steps] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(vararg steps: Any): Pair<EntriesState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntriesProcessor(DB, settings, EntrySettingsEditor(settings, database), appearance, navigator, log)

        processor.testWithInternalState(this) {
            steps.forEach { step ->
                when (step) {
                    is EntriesAction -> containerHost.action(step).join()
                    is FailNext -> step.settings.failure = EntrySettingsFailure.Unexpected()
                }
            }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    /** From here on the core refuses - after the screen read what it shows. */
    private class FailNext(val settings: FakeEntrySettingsRepository)

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val SIX_MB = 6L * MEGABYTE
    }
}
