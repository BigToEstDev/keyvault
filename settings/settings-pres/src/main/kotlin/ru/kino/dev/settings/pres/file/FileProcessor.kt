package ru.kino.dev.settings.pres.file

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.Compression
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.file.DatabaseFileEditor
import ru.kino.dev.vault.file.DatabaseFileFailure
import ru.kino.dev.vault.file.DatabaseFileInfo
import ru.kino.dev.vault.file.DatabaseFileRepository

/**
 * @param file the file of the database; null until read - the screen is a skeleton meanwhile
 * @param renaming the dialog of the name, while open
 * @param savingCompression the compression chosen is written into the file - the choice waits, with progress
 * @param message for the snackbar, until it takes it
 */
data class FileState(
    val file: DatabaseFileInfo? = null,
    val renaming: RenameDialog? = null,
    val savingCompression: Boolean = false,
    val message: FileMessage? = null,
)

/**
 * The file of the database [dbKey] (pass-docs, Step 63): its name inside the file, the name of the file, the
 * format, the compression. Kept in the file, not on the device: a change is made in the database and the file
 * written at once - the save takes as long as the key derivation of the database, so it shows progress. Not
 * written - the change is made all the same and waits for the next save; the screen shows it and says so.
 */
class FileProcessor(
    private val dbKey: String,
    private val files: DatabaseFileRepository,
    private val editor: DatabaseFileEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<FileState, FileState, Nothing> {

    override val container = orbitContainer<FileState, Nothing>(FileState())

    fun action(action: FileAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is FileAction.RenameChanged -> textIntent {
            reduce {
                val dialog = state.renaming
                if (dialog == null || dialog.saving) state else state.copy(renaming = dialog.copy(name = action.name, empty = false))
            }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<FileState, Nothing>.handle(action: FileAction) {
        when (action) {
            FileAction.Load -> try {
                val file = files.read(dbKey)
                reduce { state.copy(file = file) }
            } catch (failure: DatabaseFileFailure) {
                failed(failure)
            }

            FileAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            FileAction.Rename -> state.file?.let { file ->
                if (!state.savingCompression) reduce { state.copy(renaming = RenameDialog(file.name)) }
            }

            FileAction.RenameConfirm -> rename()

            FileAction.RenameDismiss -> if (state.renaming?.saving == false) reduce { state.copy(renaming = null) }

            is FileAction.SelectCompression -> selectCompression(action.compression)

            FileAction.MessageShown -> reduce { state.copy(message = null) }

            // Typed text has its own intent above
            is FileAction.RenameChanged -> Unit
        }
    }

    private suspend fun Syntax<FileState, Nothing>.rename() {
        val dialog = state.renaming ?: return
        if (dialog.saving) return

        reduce { state.copy(renaming = dialog.copy(saving = true)) }
        try {
            val kept = editor.rename(dbKey, dialog.name)
            reduce { state.copy(file = state.file?.copy(name = kept), renaming = null) }
        } catch (_: DatabaseFileFailure.EmptyName) {
            reduce { state.copy(renaming = dialog.copy(saving = false, empty = true)) }
        } catch (_: DatabaseFileFailure.NotSaved) {
            // The database has the name: it is written with the next save
            reduce {
                state.copy(
                    file = state.file?.copy(name = dialog.name.trim()),
                    renaming = null,
                    message = FileMessage.NotSaved,
                )
            }
        } catch (failure: DatabaseFileFailure) {
            reduce { state.copy(renaming = null) }
            failed(failure)
        }
    }

    private suspend fun Syntax<FileState, Nothing>.selectCompression(compression: Compression) {
        val file = state.file ?: return
        if (state.savingCompression || compression == file.compression) return

        reduce { state.copy(file = file.copy(compression = compression), savingCompression = true) }
        try {
            editor.setCompression(dbKey, compression)
            reduce { state.copy(savingCompression = false) }
        } catch (_: DatabaseFileFailure.NotSaved) {
            // The database has it: it is written with the next save
            reduce { state.copy(savingCompression = false, message = FileMessage.NotSaved) }
        } catch (failure: DatabaseFileFailure) {
            reduce { state.copy(file = file, savingCompression = false) }
            failed(failure)
        }
    }

    private suspend fun Syntax<FileState, Nothing>.failed(failure: DatabaseFileFailure) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = FileMessage.Failed) }
    }

    private companion object {
        const val TAG = "FileProcessor"
    }
}
