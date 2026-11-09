package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateFileNames
import ru.kino.dev.welcome.pres.create.CreateStage
import ru.kino.dev.welcome.pres.create.CreateSteps
import ru.kino.dev.welcome.pres.create.Overwrite
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute

/**
 * The key file of a new database - only when creating with one. Either a new one, generated and saved where
 * the user says, or a file already on the device, picked as it is.
 *
 * A new one is saved before the database is created, so that an app closed on this screen does not leave a
 * database with no key anywhere; it cannot be skipped. An existing file is never written to - it is only
 * checked to be there and not too large, right away rather than after "save as" for the database. Then the
 * screen says the key file is ready and waits for "next" - "save as" for the database is not launched on
 * its own, two system dialogs in a row flicker (pass-docs, Step 34, item 9) - and `create` from here, with
 * the 2.5 s wait. An existing file is never
 * written over without asking: as a key file it may be the key of another database.
 *
 * The screen is laid out as pass-docs `docs/app/mvi-arc.md` says (Step 95): the actions are [KeyFileBackupActions],
 * done here as methods.
 */
class KeyFileBackupProcessor(
    private val draft: CreateDraft,
    private val steps: CreateSteps,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<KeyFileBackupState, KeyFileBackupState, KeyFileBackupSideEffect>,
    KeyFileBackupActions {

    override val container =
        orbitContainer<KeyFileBackupState, KeyFileBackupSideEffect>(KeyFileBackupState()) {
            // After the process was killed the draft is empty - there is no password to create with. Said first, and
            // back to the master password on "OK": leaving at once would lose what the picker hands back, and the
            // file "save as" made would stay (pass-docs, Step 112)
            if (draft.password == null) reduce { state.copy(stage = CreateStage(interrupted = true)) }
        }

    /** The main button: create a new key file, or - once there is one - "next", where the database goes. */
    override fun onContinue() {
        intent {
            postSideEffect(
                if (state.keyFile != null) {
                    KeyFileBackupSideEffect.PickDatabase(CreateFileNames.DATABASE)
                } else {
                    KeyFileBackupSideEffect.PickKeyFile(CreateFileNames.KEY_FILE)
                },
            )
        }
    }

    override fun onPickExisting() {
        intent { postSideEffect(KeyFileBackupSideEffect.PickExistingKeyFile) }
    }

    override fun onKeyFilePicked(uri: String) {
        intent { picked(Overwrite(PickedFile.KeyFile, uri)) }
    }

    override fun onExistingPicked(uri: String) {
        intent { existingPicked(uri) }
    }

    override fun onDatabasePicked(uri: String) {
        intent { picked(Overwrite(PickedFile.Database, uri)) }
    }

    override fun onOverwriteConfirmed() {
        intent {
            val overwrite = state.stage.overwrite ?: return@intent
            reduce { state.copy(stage = state.stage.copy(overwrite = null)) }
            write(overwrite)
        }
    }

    override fun onOverwriteDismissed() {
        intent { reduce { state.copy(stage = state.stage.copy(overwrite = null)) } }
    }

    override fun onInterruptedDismissed() {
        intent { navigator.navigate(MviNavEvent.NavigateBackTo(MasterPasswordRoute)) }
    }

    /** An existing file as the key file: checked, not written; then straight on to the database. */
    private suspend fun Syntax<KeyFileBackupState, KeyFileBackupSideEffect>.existingPicked(uri: String) {
        // An existing file is the user's whatever is in it: never deleted
        if (draft.password == null || state.stage.busy) return

        reduce { state.copy(stage = CreateStage(busy = true)) }
        try {
            steps.checkKeyFile(uri)
            draft.keyFileSaved(uri)
            val name = steps.displayName(uri)
            reduce { state.copy(keyFile = KeyFileChoice.Picked(name), stage = CreateStage()) }
        } catch (failure: WelcomeFailure) {
            reduce { state.copy(stage = CreateStage(problem = steps.problemOf(failure, TAG))) }
        }
    }

    /** A file picked in "save as": asked about first when it is not empty, written otherwise. */
    private suspend fun Syntax<KeyFileBackupState, KeyFileBackupSideEffect>.picked(file: Overwrite) {
        if (state.stage.busy) return
        // The killed process took the draft with it: the file "save as" made is deleted if empty
        if (draft.password == null) {
            steps.discardAbandoned(file.uri)
            reduce { state.copy(stage = CreateStage(interrupted = true)) }
            return
        }

        reduce { state.copy(stage = CreateStage(busy = true)) }
        try {
            if (steps.wouldOverwrite(file.uri, file.file)) {
                reduce { state.copy(stage = CreateStage(overwrite = file)) }
            } else {
                write(file)
            }
        } catch (failure: WelcomeFailure) {
            reduce { state.copy(stage = CreateStage(problem = steps.problemOf(failure, TAG))) }
        }
    }

    private suspend fun Syntax<KeyFileBackupState, KeyFileBackupSideEffect>.write(file: Overwrite) {
        val password = draft.password ?: return

        reduce { state.copy(stage = CreateStage(busy = true)) }
        try {
            when (file.file) {
                PickedFile.KeyFile -> {
                    steps.writeNewKeyFile(file.uri)
                    draft.keyFileSaved(file.uri)
                    reduce { state.copy(keyFile = KeyFileChoice.Created, stage = CreateStage()) }
                }

                PickedFile.Database -> steps.create(file.uri, password, draft.keyFileUri)
            }
        } catch (failure: WelcomeFailure) {
            reduce { state.copy(stage = CreateStage(problem = steps.problemOf(failure, TAG))) }
        }
    }

    private companion object {
        const val TAG = "KeyFileBackupProcessor"
    }
}
