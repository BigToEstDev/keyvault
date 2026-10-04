package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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

/** @param keyFileSaved the key file is on disk; what is left is the database */
data class KeyFileBackupState(
    val keyFileSaved: Boolean = false,
    val stage: CreateStage = CreateStage(),
)

/**
 * Saving the new key file - only when creating with one, and it cannot be skipped: there is no other copy,
 * and without it the database never opens again.
 *
 * The key file is saved before the database is created, so that an app closed on this screen does not
 * leave a database with no key anywhere. Then "save as" for the database and `create` from here, with the
 * 2.5 s wait. An existing file is never written over without asking: as a key file it may be the key of
 * another database.
 */
@HiltViewModel(assistedFactory = KeyFileBackupProcessor.Factory::class)
class KeyFileBackupProcessor @AssistedInject constructor(
    @Assisted private val draft: CreateDraft,
    private val steps: CreateSteps,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<KeyFileBackupState, KeyFileBackupState, KeyFileBackupSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(draft: CreateDraft): KeyFileBackupProcessor
    }

    override val container =
        orbitContainer<KeyFileBackupState, KeyFileBackupSideEffect>(KeyFileBackupState()) {
            // After the process was killed the draft is empty - there is no password to create with
            if (draft.password == null) navigator.navigate(MviNavEvent.NavigateBackTo(MasterPasswordRoute))
        }

    fun action(action: KeyFileBackupAction) = intent {
        when (action) {
            KeyFileBackupAction.Continue -> postSideEffect(
                if (state.keyFileSaved) {
                    KeyFileBackupSideEffect.PickDatabase(CreateFileNames.DATABASE)
                } else {
                    KeyFileBackupSideEffect.PickKeyFile(CreateFileNames.KEY_FILE)
                },
            )

            is KeyFileBackupAction.KeyFilePicked -> picked(Overwrite(PickedFile.KeyFile, action.uri))

            is KeyFileBackupAction.DatabasePicked -> picked(Overwrite(PickedFile.Database, action.uri))

            KeyFileBackupAction.OverwriteConfirmed -> {
                val overwrite = state.stage.overwrite ?: return@intent
                reduce { state.copy(stage = state.stage.copy(overwrite = null)) }
                write(overwrite)
            }

            KeyFileBackupAction.OverwriteDismissed -> reduce {
                state.copy(stage = state.stage.copy(overwrite = null))
            }
        }
    }

    /** A file picked: asked about first when it is not empty, written otherwise. */
    private suspend fun Syntax<KeyFileBackupState, KeyFileBackupSideEffect>.picked(file: Overwrite) {
        if (draft.password == null || state.stage.busy) return

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
                    reduce { state.copy(keyFileSaved = true, stage = CreateStage()) }
                    // Straight on: the database is the only thing left
                    postSideEffect(KeyFileBackupSideEffect.PickDatabase(CreateFileNames.DATABASE))
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
