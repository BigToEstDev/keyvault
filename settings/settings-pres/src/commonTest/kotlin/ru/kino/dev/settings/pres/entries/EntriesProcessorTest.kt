package ru.kino.dev.settings.pres.entries

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.phone.FakePhoneNumbers
import ru.kino.dev.core.phone.FakePhoneSettingsRepository
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.appearance.FakeAppearanceRepository
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.settings.pres.testing.runEach
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.entries.EntrySettingsEditor
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository

class EntriesProcessorTest {

    private val settings = FakeEntrySettingsRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private var appearance = FakeAppearanceRepository(initialUserNameInList = true)
    private val phoneSettings = FakePhoneSettingsRepository()
    private val phones = FakePhoneNumbers()

    @Test
    fun `the screen shown reads the settings`() = runTest {
        val (state, _) = perform({ onLoad() })

        assertEquals(settings.entries, state.entries)
        assertEquals(10, state.keptItems)
        assertEquals(SIX_MB, state.keptSize)
    }

    @Test
    fun `settings that cannot be read say so and go to the log`() = runTest {
        settings.failure = EntrySettingsFailure.Unexpected()

        val (state, _) = perform({ onLoad() })

        assertNull(state.entries)
        assertEquals(EntriesMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a hidden field switched on is written at once`() = runTest {
        val protection = FieldProtection.DEFAULT.copy(title = true)

        val (state, _) = perform({ onLoad() }, { onSetProtection(protection) })

        assertEquals(protection, state.entries?.protection)
        assertEquals(protection, settings.entries.protection)
        assertNull(state.saving)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the user name made private takes it out of the list`() = runTest {
        perform({ onLoad() }, { onSetProtection(FieldProtection.DEFAULT.copy(userName = true)) })

        assertEquals(listOf(false), appearance.savedUserNameInList)
    }

    @Test
    fun `the user name made private but not written takes it out of the list too`() = runTest {
        database.saveFailure = IOException("provider gone")

        perform({ onLoad() }, { onSetProtection(FieldProtection.DEFAULT.copy(userName = true)) })

        assertEquals(listOf(false), appearance.savedUserNameInList)
    }

    @Test
    fun `the user name refused by the core leaves the list as it was`() = runTest {
        perform({ onLoad() }, failNext(settings), { onSetProtection(FieldProtection.DEFAULT.copy(userName = true)) })

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `another field made private leaves the list as it was`() = runTest {
        perform({ onLoad() }, { onSetProtection(FieldProtection.DEFAULT.copy(url = true)) })

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `the list not showing user names is not written again`() = runTest {
        appearance = FakeAppearanceRepository(initialUserNameInList = false)

        perform({ onLoad() }, { onSetProtection(FieldProtection.DEFAULT.copy(userName = true)) })

        assertEquals(emptyList<Boolean>(), appearance.savedUserNameInList)
    }

    @Test
    fun `a higher limit is written without asking`() = runTest {
        val (state, _) = perform({ onLoad() }, { onSelectItems(20) })

        assertNull(state.trimming)
        assertEquals(HistoryLimits(20, SIX_MB), settings.entries.history)
    }

    @Test
    fun `a lower limit asks first, nothing changed meanwhile`() = runTest {
        val (state, _) = perform({ onLoad() }, { onSelectItems(5) })

        assertEquals(TrimDialog(HistoryLimits(5, SIX_MB), EntriesPart.Items), state.trimming)
        assertEquals(HistoryLimits(10, SIX_MB), settings.entries.history)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `trim confirmed keeps the lower limit and writes the file`() = runTest {
        val (state, _) = perform({ onLoad() }, { onSelectSize(2 * MEGABYTE) }, { onTrimConfirm() })

        assertNull(state.trimming)
        assertEquals(HistoryLimits(10, 2 * MEGABYTE), state.entries?.history)
        assertEquals(HistoryLimits(10, 2 * MEGABYTE), settings.entries.history)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `trim dismissed keeps the limit as it was`() = runTest {
        val (state, _) = perform({ onLoad() }, { onSelectItems(5) }, { onTrimDismiss() })

        assertNull(state.trimming)
        assertEquals(HistoryLimits(10, SIX_MB), state.entries?.history)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `no limit is written without asking`() = runTest {
        val (state, _) = perform({ onLoad() }, { onLimitItems(false) })

        assertEquals(HistoryLimits(null, SIX_MB), settings.entries.history)
        assertEquals(10, state.keptItems)
    }

    @Test
    fun `a limit switched on where there was none takes the default and asks first`() = runTest {
        settings.entries = settings.entries.copy(history = HistoryLimits(null, null))

        val (state, _) = perform({ onLoad() }, { onLimitSize(true) })

        assertEquals(TrimDialog(HistoryLimits(null, 10 * MEGABYTE), EntriesPart.Size), state.trimming)
    }

    @Test
    fun `the change made but not written stays shown and says so`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform({ onLoad() }, { onSelectItems(20) })

        assertEquals(HistoryLimits(20, SIX_MB), state.entries?.history)
        assertEquals(EntriesMessage.NotSaved, state.message)
        assertNull(state.saving)
    }

    @Test
    fun `a change the core refused goes back and says so`() = runTest {
        val protection = FieldProtection.DEFAULT.copy(notes = true)

        val (state, _) = perform(
            { onLoad() },
            failNext(settings),
            { onSetProtection(protection) },
        )

        assertEquals(FieldProtection.DEFAULT, state.entries?.protection)
        assertEquals(EntriesMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the wheel opens on the value tapped`() = runTest {
        val (state, _) = perform({ onLoad() }, { onChoose(HistoryLimit.Size) })

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

    // The prefix of a new phone field (pass-docs, Step 77) - the device here is of NL

    @Test
    fun `the prefix is read - not remembered, of the device`() = runTest {
        val (state, _) = perform({ onLoad() })

        assertEquals(PhonePrefix(remember = false, region = "NL"), state.phone)
        assertEquals(listOf("NL"), phones.prepared)
    }

    @Test
    fun `remembering the prefix is kept`() = runTest {
        perform({ onLoad() }, { onRememberPrefix(true) })

        assertTrue(phoneSettings.rememberPrefix.first())
    }

    @Test
    fun `the prefix asks for the country while it is remembered`() = runTest {
        phoneSettings.saveRememberPrefix(true)

        val (state, _) = perform({ onLoad() }, { onChoosePrefix() })

        assertEquals(phones.countries(), state.phone?.countries)
    }

    @Test
    fun `the country chosen is kept and shown, the choice closed`() = runTest {
        phoneSettings.saveRememberPrefix(true)

        val (state, _) = perform({ onLoad() }, { onChoosePrefix() }, { onPrefixChosen("KZ") })

        assertEquals(PhonePrefix(remember = true, region = "KZ"), state.phone)
        assertEquals("KZ", phoneSettings.prefixRegion.first())
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(vararg actions: EntriesActions.() -> Unit): Pair<EntriesState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = EntriesProcessor(
            DB,
            settings,
            EntrySettingsEditor(settings, database),
            appearance,
            phoneSettings,
            PhoneDefaults { phoneSettings.prefixRegion.first().takeIf { phoneSettings.rememberPrefix.first() } ?: "NL" },
            phones,
            navigator,
            log,
        )

        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    /** From here on the core refuses - after the screen read what it shows. */
    private fun failNext(settings: FakeEntrySettingsRepository): EntriesActions.() -> Unit =
        { settings.failure = EntrySettingsFailure.Unexpected() }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val SIX_MB = 6L * MEGABYTE
    }
}
