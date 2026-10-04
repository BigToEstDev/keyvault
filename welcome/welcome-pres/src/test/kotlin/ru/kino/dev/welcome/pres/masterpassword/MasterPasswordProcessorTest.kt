package ru.kino.dev.welcome.pres.masterpassword

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.FakeBiometrics
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.FakeWelcomeDatabase
import ru.kino.dev.welcome.pres.FakeWelcomeFiles
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateStage
import ru.kino.dev.welcome.pres.create.CreateSteps
import ru.kino.dev.welcome.pres.create.Overwrite
import ru.kino.dev.welcome.pres.navigation.BiometricSetupRoute
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
import ru.kino.dev.welcome.pres.navigation.KeyFileBackupRoute
import ru.kino.dev.welcome.pres.recordNavigation

class MasterPasswordProcessorTest {

    private val journal = mutableListOf<String>()
    private val database = FakeWelcomeDatabase(journal)
    private val files = FakeWelcomeFiles(journal)
    private val draft = CreateDraft()
    private val biometrics = FakeBiometrics()

    private fun TestScope.processor() =
        recordNavigation().let { (navigator, events) ->
            val steps = CreateSteps(database, files, biometrics, navigator, FakeErrorLog())
            MasterPasswordProcessor(draft, steps, navigator) to events
        }

    @Test
    fun `next is off until both fields match, and a hidden mismatch shows only after leaving the field`() {
        val typed = MasterPasswordState(password = "pw", confirm = "p")
        assertFalse(typed.canGoOn)
        assertFalse(typed.mismatch)
        assertTrue(typed.copy(confirmLeft = true).mismatch)
        assertTrue(typed.copy(confirm = "pw").canGoOn)
        assertFalse(MasterPasswordState().canGoOn)
    }

    @Test
    fun `a visible password still needs the confirmation`() {
        val visible = MasterPasswordState(password = "pw", passwordVisible = true, confirmLeft = true)
        assertFalse(visible.canGoOn)
        assertFalse(visible.mismatch)

        val mistyped = visible.copy(confirm = "pv")
        assertFalse(mistyped.canGoOn)
        assertTrue(mistyped.mismatch)

        assertTrue(visible.copy(confirm = "pw").canGoOn)
    }

    @Test
    fun `a password over the limit is not taken, and says why`() = runTest {
        val (processor, _) = processor()
        val atLimit = "x".repeat(CredentialLimits.PASSWORD_MAX_CHARS)

        processor.testWithInternalState(this) {
            containerHost.action(MasterPasswordAction.PasswordChanged(atLimit))
            expectInternalState { copy(password = atLimit) }

            containerHost.action(MasterPasswordAction.PasswordChanged(atLimit + "x"))
            expectInternalState { copy(tooLong = true) }
        }
    }

    @Test
    fun `typed text is in the state before the action returns - or the cursor of the field jumps`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            containerHost.action(MasterPasswordAction.PasswordChanged("pw"))
            containerHost.action(MasterPasswordAction.ConfirmChanged("p"))

            // Read straight away, without waiting for anything
            assertEquals("pw", containerHost.container.stateFlow.value.password)
            assertEquals("p", containerHost.container.stateFlow.value.confirm)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the limit counts code points, as the core does, not chars`() = runTest {
        val (processor, _) = processor()
        // An emoji is two chars and one code point: at the limit by code points, over it by chars
        val emojis = "🔑".repeat(CredentialLimits.PASSWORD_MAX_CHARS)

        processor.testWithInternalState(this) {
            containerHost.action(MasterPasswordAction.PasswordChanged(emojis))
            expectInternalState { copy(password = emojis) }
        }
    }

    @Test
    fun `without a key file next asks where to save the database`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.Next)
            expectSideEffect(MasterPasswordSideEffect.PickDatabase("kdbxvault.kdbx"))
        }

        assertEquals("pw", draft.password)
    }

    @Test
    fun `with a key file next goes to saving it, and the draft knows`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, ready().copy(withKeyFile = true)) {
            containerHost.action(MasterPasswordAction.Next).join()
        }

        assertTrue(draft.withKeyFile)
        assertEquals(listOf(MviNavEvent.NavigateTo(KeyFileBackupRoute)), events)
    }

    @Test
    fun `a new file is created right away, and the creation graph goes`() = runTest {
        draft.start("pw", withKeyFile = false)
        val (processor, events) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.DatabasePicked(DB)).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("wouldOverwrite $DB", "create $DB"), journal)
        assertEquals("pw", database.passwordReceived)
        assertEquals(null, database.keyFileReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(BiometricSetupRoute(DB), popUpTo = MasterPasswordRoute, inclusive = true)),
            events,
        )
    }

    @Test
    fun `without strong biometrics on the phone creating goes straight to vault`() = runTest {
        draft.start("pw", withKeyFile = false)
        biometrics.available = false
        val (processor, events) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.DatabasePicked(DB)).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntriesListRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    @Test
    fun `an existing file is asked about, and nothing is written without a yes`() = runTest {
        draft.start("pw", withKeyFile = false)
        files.existing += DB
        val (processor, _) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.DatabasePicked(DB))
            expectInternalState { copy(stage = CreateStage(busy = true)) }
            expectInternalState { copy(stage = CreateStage(overwrite = Overwrite(PickedFile.Database, DB))) }

            containerHost.action(MasterPasswordAction.OverwriteDismissed)
            expectInternalState { copy(stage = CreateStage()) }
        }

        assertEquals(listOf("wouldOverwrite $DB"), journal)
    }

    @Test
    fun `a yes to writing over creates the database`() = runTest {
        draft.start("pw", withKeyFile = false)
        val (processor, _) = processor()
        val asking = ready().copy(stage = CreateStage(overwrite = Overwrite(PickedFile.Database, DB)))

        processor.testWithInternalState(this, asking) {
            containerHost.action(MasterPasswordAction.OverwriteConfirmed).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("create $DB"), journal)
    }

    @Test
    fun `a file that cannot be written is shown, and the button picks again`() = runTest {
        draft.start("pw", withKeyFile = false)
        database.failure = WelcomeFailure.FileUnreachable(PickedFile.Database)
        val (processor, events) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.DatabasePicked(DB))
            expectInternalState { copy(stage = CreateStage(busy = true)) }
            expectInternalState {
                copy(stage = CreateStage(problem = EntryProblem.Unreachable(PickedFile.Database)))
            }
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a file picked after the process was killed is left alone - the draft is empty`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, ready()) {
            containerHost.action(MasterPasswordAction.DatabasePicked(DB)).join()
        }

        assertTrue(journal.isEmpty())
    }

    private fun ready() = MasterPasswordState(password = "pw", confirm = "pw")

    private companion object {
        const val DB = "content://docs/kdbxvault.kdbx"
    }
}
