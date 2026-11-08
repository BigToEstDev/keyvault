package ru.kino.dev.settings.pres.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.settings.pres.testing.runEach
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.sync.FakeOtherDatabaseRepository
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.OtherDatabaseCredentials
import ru.kino.dev.vault.sync.OtherDatabaseEditor
import ru.kino.dev.vault.sync.OtherDatabaseKind
import ru.kino.dev.vault.sync.SaveStatus

class MergeProcessorTest {

    private val other = FakeOtherDatabaseRepository().apply {
        names[FILE] = "passwords-phone.kdbx"
        names[KEY] = "key.keyx"
    }
    private val database = FakeVaultDatabase()
    private val status = SaveStatus()
    private val log = RecordingLog()

    private val copyDialog = CredentialsDialog(OtherDatabaseKind.Copy, FILE, "passwords-phone.kdbx")
    private val bothWaysDialog =
        CredentialsDialog(OtherDatabaseKind.CopyBothWays, FILE, "passwords-phone.kdbx", password = "pw")

    @Test
    fun `a file picked on a card asks its credentials, its name shown`() = runTest {
        val (state, _) = perform({ onDatabasePicked(OtherDatabaseKind.Import, FILE) })

        assertEquals(CredentialsDialog(OtherDatabaseKind.Import, FILE, "passwords-phone.kdbx"), state.credentials)
    }

    @Test
    fun `another file picked from the open dialog keeps what was typed`() = runTest {
        val typed = copyDialog.copy(password = "pw", keyFileUri = KEY, keyFileName = "key.keyx", wrongCredentials = true)

        val (state, _) = perform({ onDatabasePicked(OtherDatabaseKind.Copy, OTHER) }, initial = MergeState(credentials = typed))

        assertEquals(typed.copy(uri = OTHER, fileName = null, wrongCredentials = false), state.credentials)
    }

    @Test
    fun `nothing typed - nothing to try`() = runTest {
        val (state, _) = perform({ onDatabasePicked(OtherDatabaseKind.Copy, FILE) }, { onConfirm() })

        assertTrue(other.calls.isEmpty())
        assertFalse(state.credentials!!.canConfirm)
    }

    @Test
    fun `a copy is merged with the password typed and what came in is said`() = runTest {
        val (state, _) = perform(
            { onDatabasePicked(OtherDatabaseKind.Copy, FILE) },
            { onPasswordChange("copy password") },
            { onConfirm() },
        )

        assertEquals(listOf("mergeCopy $FILE"), other.calls)
        assertEquals(OtherDatabaseCredentials("copy password", null), other.credentialsReceived)
        assertEquals(listOf("save"), database.calls)
        assertNull(state.credentials)
        assertEquals(MergeMessage.Done(other.summary), state.message)
    }

    @Test
    fun `a key file alone opens the other database`() = runTest {
        perform(
            { onDatabasePicked(OtherDatabaseKind.Import, FILE) },
            { onKeyFilePicked(KEY) },
            { onConfirm() },
        )

        assertEquals(listOf("importDatabase $FILE"), other.calls)
        assertEquals(OtherDatabaseCredentials(null, KEY), other.credentialsReceived)
    }

    @Test
    fun `the key file picked shows its name, and goes when removed`() = runTest {
        val (state, _) = perform(
            { onDatabasePicked(OtherDatabaseKind.Copy, FILE) },
            { onKeyFilePicked(KEY) },
            { onRemoveKeyFile() },
        )

        val dialog = checkNotNull(state.credentials)
        assertNull(dialog.keyFileUri)
        assertNull(dialog.keyFileName)
    }

    @Test
    fun `wrong credentials are said in the dialog, which stays`() = runTest {
        other.failure = MergeFailure.WrongCredentials()

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = copyDialog.copy(password = "no")))

        assertEquals(copyDialog.copy(password = "no", wrongCredentials = true), state.credentials)
        assertNull(state.message)
    }

    @Test
    fun `typing again clears what was said under the field`() = runTest {
        val wrong = copyDialog.copy(password = "no", wrongCredentials = true)

        val (state, _) = perform({ onPasswordChange("yes") }, initial = MergeState(credentials = wrong))

        assertFalse(state.credentials!!.wrongCredentials)
    }

    @Test
    fun `a password past the limit is not taken`() = runTest {
        val long = "x".repeat(CredentialLimits.PASSWORD_MAX_CHARS + 1)

        val (state, _) = perform({ onPasswordChange(long) }, initial = MergeState(credentials = copyDialog))

        val dialog = checkNotNull(state.credentials)
        assertEquals("", dialog.password)
        assertTrue(dialog.tooLong)
    }

    @Test
    fun `the wrong card is said, the dialog stays to try again`() = runTest {
        other.failure = MergeFailure.NotSameDatabase()

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = copyDialog.copy(password = "pw")))

        assertEquals(MergeMessage.Failed(MergeProblem.NotSameDatabase), state.message)
        assertEquals(copyDialog.copy(password = "pw"), state.credentials)
        assertTrue(database.calls.isEmpty())
    }

    @Test
    fun `a merge not written is said, the dialog goes`() = runTest {
        database.saveFailure = DatabaseFileException("disk full")

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = copyDialog.copy(password = "pw")))

        assertEquals(MergeMessage.Failed(MergeProblem.NotSaved), state.message)
        assertNull(state.credentials)
    }

    @Test
    fun `a failure not expected goes to the log`() = runTest {
        other.failure = MergeFailure.Unexpected(IllegalStateException("bug"))

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = copyDialog.copy(password = "pw")))

        assertEquals(MergeMessage.Failed(MergeProblem.Unexpected), state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `Cancel closes the dialog of the credentials`() = runTest {
        val (state, _) = perform({ onDismissCredentials() }, initial = MergeState(credentials = copyDialog))

        assertNull(state.credentials)
    }

    @Test
    fun `while the merge runs the dialog does not close`() = runTest {
        val (state, _) = perform({ onDismissCredentials() }, initial = MergeState(credentials = copyDialog.copy(merging = true)))

        assertTrue(state.credentials!!.merging)
    }

    @Test
    fun `OK closes what was said`() = runTest {
        val (state, _) = perform({ onDismissMessage() }, initial = MergeState(message = MergeMessage.Done(other.summary)))

        assertNull(state.message)
    }

    @Test
    fun `the file of this database is said when picked, no credentials asked`() = runTest {
        val (state, _) = perform({ onDatabasePicked(OtherDatabaseKind.CopyBothWays, DB) })

        assertEquals(MergeMessage.Failed(MergeProblem.OwnFile), state.message)
        assertNull(state.credentials)
        assertTrue(other.calls.isEmpty())
    }

    @Test
    fun `both ways the copy is merged, this database written, then the copy, what came in said`() = runTest {
        val (state, _) = perform(
            { onDatabasePicked(OtherDatabaseKind.CopyBothWays, FILE) },
            { onPasswordChange("copy password") },
            { onConfirm() },
        )

        assertEquals(listOf("mergeCopyBothWays $FILE", "writeCopy $FILE"), other.calls)
        assertEquals(listOf("save"), database.calls)
        assertNull(state.credentials)
        assertEquals(MergeMessage.Done(other.summary), state.message)
    }

    @Test
    fun `both ways a copy not written is said, the dialog goes`() = runTest {
        other.writeFailure = MergeFailure.CopyNotWritten()

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = bothWaysDialog))

        assertEquals(MergeMessage.Failed(MergeProblem.CopyNotWritten), state.message)
        assertNull(state.credentials)
    }

    @Test
    fun `both ways this file changed elsewhere is said, the copy not written`() = runTest {
        status.changedElsewhere(DB)

        val (state, _) = perform({ onConfirm() }, initial = MergeState(credentials = bothWaysDialog))

        assertEquals(MergeMessage.Failed(MergeProblem.CopyNotWrittenChangedElsewhere), state.message)
        assertEquals(listOf("mergeCopyBothWays $FILE"), other.calls)
        assertNull(state.credentials)
    }

    @Test
    fun `back goes back`() = runTest {
        val (_, events) = perform({ onBack() })

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(
        vararg actions: MergeActions.() -> Unit,
        initial: MergeState = MergeState(),
    ): Pair<MergeState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = MergeProcessor(DB, other, OtherDatabaseEditor(other, database, status, DatabaseGuard()), navigator, log)

        processor.testWithInternalState(this, initial) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
        const val FILE = "content://downloads/passwords-phone.kdbx"
        const val KEY = "content://downloads/key.keyx"
        const val OTHER = "content://downloads/passwords-old.kdbx"
    }
}
