package ru.kino.dev.welcome.pres.keyfilebackup

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
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
import ru.kino.dev.welcome.pres.recordNavigation

class KeyFileBackupProcessorTest {

    private val journal = mutableListOf<String>()
    private val database = FakeWelcomeDatabase(journal)
    private val files = FakeWelcomeFiles(journal)
    private val draft = CreateDraft().apply { start("pw", withKeyFile = true) }
    private val biometrics = FakeBiometrics()

    private fun TestScope.processor() =
        recordNavigation().let { (navigator, events) ->
            val steps = CreateSteps(database, files, biometrics, navigator, FakeErrorLog())
            KeyFileBackupProcessor(draft, steps, navigator) to events
        }

    @Test
    fun `saving the key file comes first, then the database, then creating it with that key file`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this) {
            containerHost.action(KeyFileBackupAction.Continue)
            expectSideEffect(KeyFileBackupSideEffect.PickKeyFile("kdbxvault.keyx"))

            containerHost.action(KeyFileBackupAction.KeyFilePicked(KEY))
            expectInternalState { copy(stage = CreateStage(busy = true)) }
            expectInternalState { copy(keyFileSaved = true, stage = CreateStage()) }
            expectSideEffect(KeyFileBackupSideEffect.PickDatabase("kdbxvault.kdbx"))

            containerHost.action(KeyFileBackupAction.DatabasePicked(DB)).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(
            listOf("wouldOverwrite $KEY", "writeNewKeyFile $KEY", "wouldOverwrite $DB", "create $DB"),
            journal,
        )
        assertEquals(KEY, database.keyFileReceived)
        assertEquals("pw", database.passwordReceived)
        assertEquals(
            listOf(MviNavEvent.NavigateTo(BiometricSetupRoute(DB), popUpTo = MasterPasswordRoute, inclusive = true)),
            events,
        )
    }

    @Test
    fun `an existing file as the key file is asked about - it may be the key of another database`() = runTest {
        files.existing += KEY
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            containerHost.action(KeyFileBackupAction.KeyFilePicked(KEY))
            expectInternalState { copy(stage = CreateStage(busy = true)) }
            expectInternalState { copy(stage = CreateStage(overwrite = Overwrite(PickedFile.KeyFile, KEY))) }

            containerHost.action(KeyFileBackupAction.OverwriteDismissed)
            expectInternalState { copy(stage = CreateStage()) }
        }

        assertEquals(listOf("wouldOverwrite $KEY"), journal)
        assertNull(draft.keyFileUri)
    }

    @Test
    fun `once the key file is saved the button asks for the database`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this, KeyFileBackupState(keyFileSaved = true)) {
            containerHost.action(KeyFileBackupAction.Continue)
            expectSideEffect(KeyFileBackupSideEffect.PickDatabase("kdbxvault.kdbx"))
        }
    }

    @Test
    fun `a key file that cannot be written is shown, and nothing is created`() = runTest {
        files.writeFailure = WelcomeFailure.FileUnreachable(PickedFile.KeyFile)
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            containerHost.action(KeyFileBackupAction.KeyFilePicked(KEY))
            expectInternalState { copy(stage = CreateStage(busy = true)) }
            expectInternalState {
                copy(stage = CreateStage(problem = EntryProblem.Unreachable(PickedFile.KeyFile)))
            }
        }

        assertNull(draft.keyFileUri)
    }

    @Test
    fun `after the process was killed the draft is empty - back to the master password`() = runTest {
        val empty = CreateDraft()
        val (navigator, events) = recordNavigation()
        val steps = CreateSteps(database, files, biometrics, navigator, FakeErrorLog())
        val processor = KeyFileBackupProcessor(empty, steps, navigator)

        processor.testWithInternalState(this) { runOnCreate().join() }

        assertEquals(listOf(MviNavEvent.NavigateBackTo(MasterPasswordRoute)), events)
    }

    private companion object {
        const val KEY = "content://keys/kdbxvault.keyx"
        const val DB = "content://docs/kdbxvault.kdbx"
    }
}
