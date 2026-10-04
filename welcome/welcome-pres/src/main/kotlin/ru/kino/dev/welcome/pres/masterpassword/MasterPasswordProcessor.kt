package ru.kino.dev.welcome.pres.masterpassword

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * @param confirmLeft the confirmation field lost focus once - a mismatch is shown only after that, not
 * while it is being typed
 * @param tooLong the last thing typed was over the limit and was not taken
 */
data class MasterPasswordState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val confirmLeft: Boolean = false,
    val tooLong: Boolean = false,
    val withKeyFile: Boolean = false,
    val stage: CreateStage = CreateStage(),
) {

    /** Shorter than the minimum - said under the field while it is, and "next" stays off. */
    val tooShort: Boolean
        get() = CredentialLimits.isPasswordTooShort(password)

    /**
     * The confirmation is required whether the password is shown or not: looking at a password and
     * confirming it are different things.
     */
    val canGoOn: Boolean
        get() = !tooShort && password == confirm && !stage.busy

    val mismatch: Boolean
        get() = confirmLeft && confirm.isNotEmpty() && confirm != password
}

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
 */
@HiltViewModel(assistedFactory = MasterPasswordProcessor.Factory::class)
class MasterPasswordProcessor @AssistedInject constructor(
    @Assisted private val draft: CreateDraft,
    private val steps: CreateSteps,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<MasterPasswordState, MasterPasswordState, MasterPasswordSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(draft: CreateDraft): MasterPasswordProcessor
    }

    override val container = orbitContainer<MasterPasswordState, MasterPasswordSideEffect>(MasterPasswordState())

    fun action(action: MasterPasswordAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is MasterPasswordAction.PasswordChanged -> textIntent {
            reduce {
                if (CredentialLimits.isPasswordTooLong(action.password)) {
                    state.copy(tooLong = true)
                } else {
                    state.copy(password = action.password, tooLong = false)
                }
            }
        }

        is MasterPasswordAction.ConfirmChanged -> textIntent {
            reduce {
                if (CredentialLimits.isPasswordTooLong(action.confirm)) state else state.copy(confirm = action.confirm)
            }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<MasterPasswordState, MasterPasswordSideEffect>.handle(action: MasterPasswordAction) {
        when (action) {
            is MasterPasswordAction.PasswordChanged, is MasterPasswordAction.ConfirmChanged -> Unit // in action()

            MasterPasswordAction.ConfirmLeft -> reduce { state.copy(confirmLeft = true) }

            MasterPasswordAction.TogglePasswordVisible -> reduce {
                state.copy(passwordVisible = !state.passwordVisible)
            }

            is MasterPasswordAction.KeyFileToggled -> reduce { state.copy(withKeyFile = action.enabled) }

            MasterPasswordAction.Next -> next()

            is MasterPasswordAction.DatabasePicked -> databasePicked(action.uri)

            MasterPasswordAction.OverwriteConfirmed -> {
                val overwrite = state.stage.overwrite ?: return
                reduce { state.copy(stage = state.stage.copy(overwrite = null)) }
                create(overwrite.uri)
            }

            MasterPasswordAction.OverwriteDismissed -> reduce { state.copy(stage = state.stage.copy(overwrite = null)) }
        }
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
