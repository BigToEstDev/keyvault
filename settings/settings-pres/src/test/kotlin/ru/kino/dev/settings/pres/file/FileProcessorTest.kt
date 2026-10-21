package ru.kino.dev.settings.pres.file

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.Compression
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.file.DatabaseFileEditor
import ru.kino.dev.vault.file.DatabaseFileFailure
import ru.kino.dev.vault.file.FakeDatabaseFileRepository
import java.io.IOException

class FileProcessorTest {

    private val files = FakeDatabaseFileRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the file`() = runTest {
        val (state, _) = perform(FileAction.Load)

        assertEquals(files.info, state.file)
    }

    @Test
    fun `a file that cannot be read says so and goes to the log`() = runTest {
        files.failure = DatabaseFileFailure.Unexpected()

        val (state, _) = perform(FileAction.Load)

        assertNull(state.file)
        assertEquals(FileMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the dialog opens with the name as it is`() = runTest {
        val (state, _) = perform(FileAction.Load, FileAction.Rename)

        assertEquals(RenameDialog("Passwords"), state.renaming)
    }

    @Test
    fun `a name saved closes the dialog and shows the name kept`() = runTest {
        val (state, _) = perform(
            FileAction.Load,
            FileAction.Rename,
            FileAction.RenameChanged("  Home "),
            FileAction.RenameConfirm,
        )

        assertNull(state.renaming)
        assertEquals("Home", state.file?.name)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `an empty name stays in the dialog with the reason`() = runTest {
        val (state, _) = perform(
            FileAction.Load,
            FileAction.Rename,
            FileAction.RenameChanged(" "),
            FileAction.RenameConfirm,
        )

        assertEquals(RenameDialog(" ", saving = false, empty = true), state.renaming)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a name not written is shown all the same, with a message`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(
            FileAction.Load,
            FileAction.Rename,
            FileAction.RenameChanged("Home"),
            FileAction.RenameConfirm,
        )

        assertNull(state.renaming)
        assertEquals("Home", state.file?.name)
        assertEquals(FileMessage.NotSaved, state.message)
    }

    @Test
    fun `a compression chosen is written at once`() = runTest {
        val (state, _) = perform(FileAction.Load, FileAction.SelectCompression(Compression.None))

        assertEquals(Compression.None, state.file?.compression)
        assertFalse(state.savingCompression)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a compression not written is shown all the same, with a message`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(FileAction.Load, FileAction.SelectCompression(Compression.None))

        assertEquals(Compression.None, state.file?.compression)
        assertFalse(state.savingCompression)
        assertEquals(FileMessage.NotSaved, state.message)
    }

    @Test
    fun `nothing is chosen before the file is read`() = runTest {
        val (state, _) = perform(FileAction.SelectCompression(Compression.None), FileAction.Rename)

        assertNull(state.renaming)
        assertEquals(emptyList<String>(), database.calls)
        assertTrue(files.changes.isEmpty())
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (_, events) = perform(FileAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(vararg actions: FileAction): Pair<FileState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = FileProcessor(DB, files, DatabaseFileEditor(files, database), navigator, log)

        processor.testWithInternalState(this) {
            actions.forEach { containerHost.action(it).join() }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
