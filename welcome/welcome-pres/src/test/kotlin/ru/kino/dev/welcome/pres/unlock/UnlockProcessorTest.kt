package ru.kino.dev.welcome.pres.unlock

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.FakeCurrentDatabase
import ru.kino.dev.welcome.pres.FakeBiometrics
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.FakeWelcomeDatabase
import ru.kino.dev.welcome.pres.FakeWelcomeFiles
import ru.kino.dev.welcome.BiometricFailure
import ru.kino.dev.welcome.pres.common.BiometricNotice
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
import ru.kino.dev.welcome.pres.navigation.WelcomeRoute
import ru.kino.dev.welcome.pres.recordNavigation

class UnlockProcessorTest {

    private val journal = mutableListOf<String>()
    private val database = FakeWelcomeDatabase(journal)
    private val files = FakeWelcomeFiles().apply { names[OTHER] = "other.kdbx" }
    private val current = FakeCurrentDatabase(REMEMBERED)
    private val log = FakeErrorLog()
    private val biometrics = FakeBiometrics(journal)

    private fun TestScope.processor(pickedUri: String? = null) =
        recordNavigation().let { (navigator, events) ->
            UnlockProcessor(pickedUri, database, files, current, navigator, log, BiometricSignIn(biometrics, log)) to events
        }

    @Test
    fun `on start the remembered database is shown with its names and its key file`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            runOnCreate().join()

            expectInternalState(rememberedState())
        }
    }

    @Test
    fun `nothing remembered and nothing picked starts over on the welcome screen`() = runTest {
        current.current.value = null
        val (processor, events) = processor()

        processor.testWithInternalState(this) { runOnCreate().join() }

        assertEquals(listOf(MviNavEvent.NavigateTo(WelcomeRoute, popUpTo = WelcomeGraph)), events)
    }

    @Test
    fun `a picked file is shown by its file name, without the remembered key file`() = runTest {
        val (processor, _) = processor(pickedUri = OTHER)

        processor.testWithInternalState(this) {
            runOnCreate().join()

            expectInternalState(UnlockState(databaseUri = OTHER, fileName = "other.kdbx"))
        }
    }

    @Test
    fun `opening goes to vault and wipes the welcome graph behind`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.action(UnlockAction.PasswordChanged("pw")).join()
            containerHost.action(UnlockAction.Submit).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals("pw", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntriesListRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `while opening the screen waits, and a second submit starts nothing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        database.gate = gate
        val (processor, _) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.action(UnlockAction.Submit)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            containerHost.action(UnlockAction.Submit).join()
            gate.complete(Unit)
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("open $DB"), journal)
    }

    @Test
    fun `an empty password with a key file is no password`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.action(UnlockAction.Submit).join()
            cancelAndIgnoreRemainingItems()
        }

        assertNull(database.passwordReceived)
    }

    @Test
    fun `an empty password without a key file is an empty password`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, UnlockState(databaseUri = DB)) {
            containerHost.action(UnlockAction.Submit).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals("", database.passwordReceived)
    }

    @Test
    fun `wrong credentials keep the password and show under it`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()

        assertFailureShows(EntryProblem.InvalidCredentials)
    }

    @Test
    fun `every failure becomes its own problem on the screen`() = runTest {
        val cases = listOf(
            WelcomeFailure.UnsupportedFile() to EntryProblem.UnsupportedFile,
            WelcomeFailure.CorruptedFile() to EntryProblem.CorruptedFile,
            WelcomeFailure.FileUnreachable(PickedFile.Database) to EntryProblem.Unreachable(PickedFile.Database),
            WelcomeFailure.FileUnreachable(PickedFile.KeyFile) to EntryProblem.Unreachable(PickedFile.KeyFile),
            WelcomeFailure.KeyFileTooLarge() to EntryProblem.KeyFileTooLarge(maxMebibytes = 10),
            WelcomeFailure.Unexpected() to EntryProblem.Unexpected,
        )
        for ((failure, problem) in cases) {
            database.failure = failure
            assertFailureShows(problem)
        }
    }

    @Test
    fun `only our own failure goes to the log`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        assertFailureShows(EntryProblem.InvalidCredentials)
        assertTrue(log.logged.isEmpty())

        database.failure = WelcomeFailure.Unexpected()
        assertFailureShows(EntryProblem.Unexpected)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `typing again clears wrong credentials`() = runTest {
        val (processor, _) = processor()
        val wrong = rememberedState().let { it.copy(entry = it.entry.copy(problem = EntryProblem.InvalidCredentials)) }

        processor.testWithInternalState(this, wrong) {
            containerHost.action(UnlockAction.PasswordChanged("pw2"))

            expectInternalState { copy(entry = entry.copy(password = "pw2", problem = null)) }
        }
    }

    @Test
    fun `another database picked here replaces what was typed and attached for this one`() = runTest {
        val (processor, _) = processor()
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.action(UnlockAction.ChangeDatabase)
            expectSideEffect(UnlockSideEffect.PickDatabase)

            containerHost.action(UnlockAction.DatabasePicked(OTHER))
            expectInternalState(UnlockState(databaseUri = OTHER, fileName = "other.kdbx"))
        }
    }

    @Test
    fun `a key file picked is attached by its name and clears a problem with the key file`() = runTest {
        files.names[NEW_KEY] = "new.keyx"
        val (processor, _) = processor()
        val tooLarge = UnlockState(
            databaseUri = DB,
            entry = PasswordEntry(keyFileUri = KEY, problem = EntryProblem.KeyFileTooLarge(10)),
        )

        processor.testWithInternalState(this, tooLarge) {
            containerHost.action(UnlockAction.AttachKeyFile)
            expectSideEffect(UnlockSideEffect.PickKeyFile)

            containerHost.action(UnlockAction.KeyFilePicked(NEW_KEY))
            expectInternalState { copy(entry = PasswordEntry(keyFileUri = NEW_KEY, keyFileName = "new.keyx")) }

            containerHost.action(UnlockAction.RemoveKeyFile)
            expectInternalState { copy(entry = PasswordEntry()) }
        }
    }

    // --- a database that is gone (Step 34, bug 1) -------------------------------------------------------------

    @Test
    fun `a remembered database deleted outside the app is said on start, before the password is typed`() =
        runTest {
            files.missing += DB
            val (processor, _) = processor()

            processor.testWithInternalState(this) {
                runOnCreate().join()
                expectInternalState(gone())
            }

            assertTrue(journal.isEmpty())
        }

    @Test
    fun `retry looks at the file again - still gone stays, back again shows the password`() = runTest {
        files.missing += DB
        val (processor, _) = processor()

        processor.testWithInternalState(this, gone()) {
            containerHost.action(UnlockAction.Retry)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false)) }

            files.missing -= DB
            containerHost.action(UnlockAction.Retry)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = null)) }
        }

        assertTrue(journal.isEmpty())
    }

    @Test
    fun `retry with a typed password opens - a sync that finished, say`() = runTest {
        val (processor, events) = processor()
        val corrupted = rememberedState().let {
            it.copy(entry = it.entry.copy(password = "pw", problem = EntryProblem.CorruptedFile))
        }

        processor.testWithInternalState(this, corrupted) {
            containerHost.action(UnlockAction.Retry).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("open $DB"), journal)
        assertEquals(1, events.size)
    }

    @Test
    fun `a new database can always be created from here - the way out when the remembered one is gone`() =
        runTest {
            val (processor, events) = processor()

            processor.testWithInternalState(this, gone()) {
                containerHost.action(UnlockAction.CreateDatabase).join()
            }

            assertEquals(listOf(MviNavEvent.NavigateTo(MasterPasswordRoute)), events)
        }

    private fun gone() = rememberedState().let {
        it.copy(entry = it.entry.copy(problem = EntryProblem.Unreachable(PickedFile.Database)))
    }

    // --- biometrics -----------------------------------------------------------------------------------------

    @Test
    fun `the biometric button is there only when biometrics are on for the database and the phone`() = runTest {
        biometrics.passwords[DB] = "pw"
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(rememberedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) })
        }

        biometrics.available = false
        val (offPhone, _) = processor()
        offPhone.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(rememberedState())
        }
    }

    @Test
    fun `biometrics give the password, and it opens the usual way, with the key file`() = runTest {
        biometrics.passwords[DB] = "stored"
        val (processor, events) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.action(UnlockAction.Biometric).join()
            val prompt = awaitSideEffect() as UnlockSideEffect.ShowBiometricPrompt
            containerHost.action(UnlockAction.BiometricFinished(BiometricOutcome.Succeeded(prompt.session))).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals("stored", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntriesListRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `a stored password that no longer opens is forgotten - the password changed`() = runTest {
        biometrics.passwords[DB] = "old"
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, events) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.action(UnlockAction.Biometric).join()
            val prompt = awaitSideEffect() as UnlockSideEffect.ShowBiometricPrompt
            containerHost.action(UnlockAction.BiometricFinished(BiometricOutcome.Succeeded(prompt.session))).join()
            cancelAndIgnoreRemainingItems()
        }

        assertTrue(journal.contains("forget $DB"))
        assertEquals(
            biometricState().entry.copy(biometricOn = false, biometricNotice = BiometricNotice.PasswordChanged),
            processor.container.stateFlow.value.entry,
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `changed biometrics of the phone turn the button off and say why`() = runTest {
        biometrics.passwords[DB] = "stored"
        biometrics.startFailure = BiometricFailure.KeyInvalidated()
        val (processor, _) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.action(UnlockAction.Biometric)
            expectInternalState {
                copy(entry = entry.copy(biometricOn = false, biometricNotice = BiometricNotice.DeviceChanged))
            }
        }
    }

    @Test
    fun `a cancelled prompt changes nothing - the password is still there to type`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.action(UnlockAction.BiometricFinished(BiometricOutcome.Cancelled)).join()
        }

        assertTrue(journal.isEmpty())
    }

    private fun biometricState() = rememberedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) }

    private suspend fun TestScope.assertFailureShows(problem: EntryProblem) {
        val (processor, events) = processor()
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.action(UnlockAction.Submit)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = problem)) }
        }
        assertTrue(events.isEmpty())
    }

    private fun rememberedState() = UnlockState(
        databaseUri = DB,
        databaseName = "Passwords",
        fileName = "passwords.kdbx",
        entry = PasswordEntry(keyFileUri = KEY, keyFileName = "my.keyx"),
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val OTHER = "content://docs/other.kdbx"
        const val KEY = "content://keys/my.keyx"
        const val NEW_KEY = "content://keys/new.keyx"
        val REMEMBERED = CurrentDatabase(DB, "passwords.kdbx", "Passwords", KEY, "my.keyx")
    }
}
