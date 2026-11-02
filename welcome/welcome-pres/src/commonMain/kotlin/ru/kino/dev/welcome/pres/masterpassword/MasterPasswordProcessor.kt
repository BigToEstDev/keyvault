package ru.kino.dev.welcome.pres.masterpassword

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateFileNames
import ru.kino.dev.welcome.pres.create.CreateStage
import ru.kino.dev.welcome.pres.create.CreateSteps
import ru.kino.dev.welcome.pres.create.Overwrite
import ru.kino.dev.welcome.pres.navigation.KeyFileBackupRoute

/**
 * Setting the master password - the first step of creating a database.
 *
 * Without a key file the database is created from here: "save as" for the database, then `create` and the
 * 2.5 s wait on this screen. With one, the key file is saved first, on the next screen, and the database is
 * created from there (pass-docs, `decisions.md`, "Создание базы").
 *
 * The password is limited when it is set, as the core limits it - in code points, not chars - so that the
 * core never has to refuse it; and it has a minimum length of the app's own. Opening has neither: a
 * database made elsewhere may have any password.
 *
 * The screen is laid out as pass-docs `docs/app/mvi-arc.md` says (Step 95): the actions are [MasterPasswordActions],
 * done here as methods.
 */
class MasterPasswordProcessor(
    private val draft: CreateDraft,
    private val steps: CreateSteps,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<MasterPasswordState, MasterPasswordState, MasterPasswordSideEffect>,
    MasterPasswordActions {

    override val container = orbitContainer<MasterPasswordState, MasterPasswordSideEffect>(MasterPasswordState())

    // Typed text: the state is updated before the field draws again, or the cursor jumps
    override fun onPasswordChange(password: String) {
        textIntent {
            reduce {
                if (CredentialLimits.isPasswordTooLong(password)) {
                    state.copy(tooLong = true)
                } else {
                    state.copy(password = password, tooLong = false)
                }
            }
        }
    }

    override fun onConfirmChange(confirm: String) {
        textIntent {
            reduce { if (CredentialLimits.isPasswordTooLong(confirm)) state else state.copy(confirm = confirm) }
        }
    }

    override fun onConfirmLeft() {
        intent { reduce { state.copy(confirmLeft = true) } }
    }

    override fun onTogglePasswordVisible() {
        intent { reduce { state.copy(passwordVisible = !state.passwordVisible) } }
    }

    override fun onKeyFileToggle(enabled: Boolean) {
        intent { reduce { state.copy(withKeyFile = enabled) } }
    }

    override fun onNext() {
        intent { next() }
    }

    override fun onDatabasePicked(uri: String) {
        intent { databasePicked(uri) }
    }

    override fun onOverwriteConfirmed() {
        intent {
            val overwrite = state.stage.overwrite ?: return@intent
            reduce { state.copy(stage = state.stage.copy(overwrite = null)) }
            create(overwrite.uri)
        }
    }

    override fun onOverwriteDismissed() {
        intent { reduce { state.copy(stage = state.stage.copy(overwrite = null)) } }
    }

    private suspend fun Syntax<MasterPasswordState, MasterPasswordSideEffect>.next() {
        if (!state.canGoOn) return

        draft.start(state.password, state.withKeyFile)
        if (state.withKeyFile) {
            navigator.navigate(MviNavEvent.NavigateTo(KeyFileBackupRoute))
        } else {
            postSideEffect(MasterPasswordSideEffect.PickDatabase(CreateFileNames.DATABASE))
        }
    }

    private suspend fun Syntax<MasterPasswordState, MasterPasswordSideEffect>.databasePicked(uri: String) {
        // The draft is gone when the process was killed while the picker was open: what was typed is gone
        // with it, and the picked file is left alone - the user types again and picks again
        if (draft.password == null || state.stage.busy) return

        reduce { state.copy(stage = CreateStage(busy = true)) }
        try {
            if (steps.wouldOverwrite(uri, PickedFile.Database)) {
                reduce { state.copy(stage = CreateStage(overwrite = Overwrite(PickedFile.Database, uri))) }
            } else {
                create(uri)
            }
        } catch (failure: WelcomeFailure) {
            reduce { state.copy(stage = CreateStage(problem = steps.problemOf(failure, TAG))) }
        }
    }

    private suspend fun Syntax<MasterPasswordState, MasterPasswordSideEffect>.create(uri: String) {
        val password = draft.password ?: return

        reduce { state.copy(stage = CreateStage(busy = true)) }
        try {
            steps.create(uri, password, keyFileUri = null)
        } catch (failure: WelcomeFailure) {
            reduce { state.copy(stage = CreateStage(problem = steps.problemOf(failure, TAG))) }
        }
    }

    private companion object {
        const val TAG = "MasterPasswordProcessor"
    }
}
