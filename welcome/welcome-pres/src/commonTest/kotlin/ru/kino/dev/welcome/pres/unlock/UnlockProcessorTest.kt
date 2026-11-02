package ru.kino.dev.welcome.pres.unlock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.signin.FakeSignInSettingsRepository
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.FakeCurrentDatabase
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.FakeWelcomeDatabase
import ru.kino.dev.welcome.pres.FakeWelcomeFiles
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
    private val signIn = FakeSignInSettingsRepository()

    private fun TestScope.processor(pickedUri: String? = null, signIn: FakeSignInSettingsRepository = this@UnlockProcessorTest.signIn) =
        recordNavigation().let { (navigator, events) ->
            UnlockProcessor(pickedUri, database, files, current, navigator, log, BiometricSignIn(biometrics, log), signIn) to
                events
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

            expectInternalState(UnlockState(databaseUri = OTHER, fileName = "other.kdbx", read = true))
        }
    }

    @Test
    fun `opening goes to vault and wipes the welcome graph behind`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.onPasswordChange("pw")
            expectInternalState { copy(entry = entry.copy(password = "pw")) }
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertEquals("pw", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(VaultRootRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `opened, the screen stays waiting as it was until it is gone - not wiped for the slide`() = runTest {
        val (processor, events) = processor()
        val typed = biometricState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(typed.copy(entry = typed.entry.copy(busy = true)), processor.container.stateFlow.value)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(VaultRootRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `while opening the screen waits, and a second submit starts nothing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        database.gate = gate
        val (processor, _) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            containerHost.onSubmit()
            gate.complete(Unit)
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("open $DB"), journal)
    }

    @Test
    fun `an empty password with a key file is no password`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, rememberedState()) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertNull(database.passwordReceived)
    }

    @Test
    fun `an empty password without a key file is an empty password`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, UnlockState(databaseUri = DB)) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
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
    fun `wrong credentials wipe the password when the settings say so - the key file stays`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, _) = processor(signIn = FakeSignInSettingsRepository(clearPasswordOnError = true))
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, password = "", problem = EntryProblem.InvalidCredentials))
            }
        }
    }

    @Test
    fun `other failures keep the password even when the settings wipe a wrong one`() = runTest {
        database.failure = WelcomeFailure.CorruptedFile()
        val (processor, _) = processor(signIn = FakeSignInSettingsRepository(clearPasswordOnError = true))
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = EntryProblem.CorruptedFile)) }
        }
    }

    @Test
    fun `typing again clears wrong credentials`() = runTest {
        val (processor, _) = processor()
        val wrong = rememberedState().let { it.copy(entry = it.entry.copy(problem = EntryProblem.InvalidCredentials)) }

        processor.testWithInternalState(this, wrong) {
            containerHost.onPasswordChange("pw2")

            expectInternalState { copy(entry = entry.copy(password = "pw2", problem = null)) }
        }
    }

    @Test
    fun `another database picked here replaces what was typed and attached for this one`() = runTest {
        val (processor, _) = processor()
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onChangeDatabase()
            expectSideEffect(UnlockSideEffect.PickDatabase)

            containerHost.onDatabasePicked(OTHER)
            expectInternalState(UnlockState(databaseUri = OTHER, fileName = "other.kdbx", read = true))
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
            containerHost.onAttachKeyFile()
            expectSideEffect(UnlockSideEffect.PickKeyFile)

            containerHost.onKeyFilePicked(NEW_KEY)
            expectInternalState { copy(entry = PasswordEntry(keyFileUri = NEW_KEY, keyFileName = "new.keyx")) }

            containerHost.onRemoveKeyFile()
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
            containerHost.onRetry()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false)) }

            files.missing -= DB
            containerHost.onRetry()
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
            containerHost.onRetry()
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(problem = null)) }
        }

        assertEquals(listOf("open $DB"), journal)
        assertEquals(1, events.size)
    }

    @Test
    fun `a new database can always be created from here - the way out when the remembered one is gone`() =
        runTest {
            val (processor, events) = processor()

            processor.testWithInternalState(this, gone()) {
                containerHost.onCreateDatabase()
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
    fun `with the auto prompt on, biometrics are asked as the screen is shown`() = runTest {
        biometrics.passwords[DB] = "pw"
        val (processor, _) = processor(signIn = FakeSignInSettingsRepository(autoBiometricPrompt = true))

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(rememberedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) })
            awaitSideEffect() as UnlockSideEffect.ShowBiometricPrompt
        }
    }

    @Test
    fun `the auto prompt asks nothing for a database without biometrics`() = runTest {
        val (processor, _) = processor(signIn = FakeSignInSettingsRepository(autoBiometricPrompt = true))

        processor.testWithInternalState(this) {
            runOnCreate().join()
            expectInternalState(rememberedState())
        }
    }

    @Test
    fun `biometrics give the password, and it opens the usual way, with the key file`() = runTest {
        biometrics.passwords[DB] = "stored"
        val (processor, events) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.onBiometric()
            val prompt = awaitSideEffect() as UnlockSideEffect.ShowBiometricPrompt
            containerHost.onBiometricFinished(BiometricOutcome.Succeeded(prompt.session))
            expectInternalState { copy(entry = entry.copy(busy = true)) }
        }

        assertEquals("stored", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(VaultRootRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `a stored password that no longer opens is forgotten - the password changed`() = runTest {
        biometrics.passwords[DB] = "old"
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, events) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.onBiometric()
            val prompt = awaitSideEffect() as UnlockSideEffect.ShowBiometricPrompt
            containerHost.onBiometricFinished(BiometricOutcome.Succeeded(prompt.session))
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, biometricOn = false, biometricNotice = BiometricNotice.PasswordChanged))
            }
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
            containerHost.onBiometric()
            expectInternalState {
                copy(entry = entry.copy(biometricOn = false, biometricNotice = BiometricNotice.DeviceChanged))
            }
        }
    }

    @Test
    fun `a cancelled prompt changes nothing - the password is still there to type`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, biometricState()) {
            containerHost.onBiometricFinished(BiometricOutcome.Cancelled)
        }

        assertTrue(journal.isEmpty())
    }

    private fun biometricState() = rememberedState().let { it.copy(entry = it.entry.copy(biometricOn = true)) }

    private suspend fun TestScope.assertFailureShows(problem: EntryProblem) {
        val (processor, events) = processor()
        val typed = rememberedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.onSubmit()
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
        read = true,
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val OTHER = "content://docs/other.kdbx"
        const val KEY = "content://keys/my.keyx"
        const val NEW_KEY = "content://keys/new.keyx"
        val REMEMBERED = CurrentDatabase(DB, "passwords.kdbx", "Passwords", KEY, "my.keyx")
    }
}
