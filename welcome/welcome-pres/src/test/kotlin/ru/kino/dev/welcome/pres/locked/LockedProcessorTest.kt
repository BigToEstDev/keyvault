package ru.kino.dev.welcome.pres.locked

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.welcome.CurrentDatabase
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.FakeCurrentDatabase
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.FakeWelcomeDatabase
import ru.kino.dev.welcome.pres.FakeWelcomeFiles
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.recordNavigation

class LockedProcessorTest {

    private val journal = mutableListOf<String>()
    private val database = FakeWelcomeDatabase(journal)
    private val files = FakeWelcomeFiles().apply { names[NEW_KEY] = "new.keyx" }
    private val current = FakeCurrentDatabase(CurrentDatabase(DB, "passwords.kdbx", "Passwords", KEY, "my.keyx"))

    private fun TestScope.processor() =
        recordNavigation().let { (navigator, events) ->
            LockedProcessor(DB, database, files, current, navigator, FakeErrorLog()) to events
        }

    @Test
    fun `shows the name of the database and the key file it was opened with - nothing else`() = runTest {
        val (processor, _) = processor()

        processor.testWithInternalState(this) {
            runOnCreate().join()

            expectInternalState(lockedState())
        }
    }

    @Test
    fun `unlocking goes back to where the user was in vault`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, lockedState()) {
            containerHost.action(LockedAction.PasswordChanged("pw")).join()
            containerHost.action(LockedAction.Submit).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("unlock $DB"), journal)
        assertEquals("pw", database.passwordReceived)
        assertEquals(KEY, database.keyFileReceived)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an unreachable key file is answered under the key file, and another one is unlocked with`() = runTest {
        database.failure = WelcomeFailure.FileUnreachable(PickedFile.KeyFile)
        val (processor, events) = processor()
        val typed = lockedState().let { it.copy(entry = it.entry.copy(password = "pw")) }

        processor.testWithInternalState(this, typed) {
            containerHost.action(LockedAction.Submit)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState {
                copy(entry = entry.copy(busy = false, problem = EntryProblem.Unreachable(PickedFile.KeyFile)))
            }

            containerHost.action(LockedAction.AttachKeyFile)
            expectSideEffect(LockedSideEffect.PickKeyFile)
            containerHost.action(LockedAction.KeyFilePicked(NEW_KEY))
            expectInternalState {
                copy(entry = entry.copy(keyFileUri = NEW_KEY, keyFileName = "new.keyx", problem = null))
            }

            containerHost.action(LockedAction.Submit).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(NEW_KEY, database.keyFileReceived)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `wrong credentials stay on the lock`() = runTest {
        database.failure = WelcomeFailure.InvalidCredentials()
        val (processor, events) = processor()

        processor.testWithInternalState(this, lockedState()) {
            containerHost.action(LockedAction.Submit)
            expectInternalState { copy(entry = entry.copy(busy = true)) }
            expectInternalState { copy(entry = entry.copy(busy = false, problem = EntryProblem.InvalidCredentials)) }
        }

        assertTrue(events.isEmpty())
    }

    private fun lockedState() = LockedState(
        databaseName = "Passwords",
        entry = PasswordEntry(keyFileUri = KEY, keyFileName = "my.keyx"),
    )

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val KEY = "content://keys/my.keyx"
        const val NEW_KEY = "content://keys/new.keyx"
    }
}
