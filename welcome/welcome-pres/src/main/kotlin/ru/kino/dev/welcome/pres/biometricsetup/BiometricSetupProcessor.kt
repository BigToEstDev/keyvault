package ru.kino.dev.welcome.pres.biometricsetup

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.BiometricFailure
import ru.kino.dev.welcome.BiometricRepository
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.create.CreateDraft

/**
 * @param busy turning biometrics on - the prompt is out, or the password is being stored
 * @param failed turning on did not work; skipping still does
 */
data class BiometricSetupState(
    val busy: Boolean = false,
    val failed: Boolean = false,
)

/**
 * The last step of creating: offering biometrics. Shown only when the phone has strong biometrics - the
 * creating screens skip it otherwise.
 *
 * The database is created and open by now, so whatever the answer, the next stop is `vault`, and the
 * password is not asked again: turning biometrics on encrypts the one still in the draft. Leaving for
 * `vault` wipes the `welcome` graph, the draft with it.
 */
@HiltViewModel(assistedFactory = BiometricSetupProcessor.Factory::class)
class BiometricSetupProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    @Assisted private val draft: CreateDraft,
    private val biometrics: BiometricRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<BiometricSetupState, BiometricSetupState, BiometricSetupSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String, draft: CreateDraft): BiometricSetupProcessor
    }

    override val container = orbitContainer<BiometricSetupState, BiometricSetupSideEffect>(BiometricSetupState())

    fun action(action: BiometricSetupAction) = intent {
        when (action) {
            BiometricSetupAction.Enable -> enable()

            is BiometricSetupAction.PromptFinished -> promptFinished(action.outcome)

            BiometricSetupAction.Skip -> toVault()
        }
    }

    private suspend fun Syntax<BiometricSetupState, BiometricSetupSideEffect>.enable() {
        if (state.busy || draft.password == null) return

        reduce { BiometricSetupState(busy = true) }
        try {
            postSideEffect(BiometricSetupSideEffect.ShowPrompt(biometrics.startSaving()))
        } catch (failure: BiometricFailure) {
            log.unexpected(TAG, failure)
            reduce { BiometricSetupState(failed = true) }
        }
    }

    private suspend fun Syntax<BiometricSetupState, BiometricSetupSideEffect>.promptFinished(
        outcome: BiometricOutcome,
    ) {
        when (outcome) {
            BiometricOutcome.Cancelled -> reduce { BiometricSetupState() }

            BiometricOutcome.Failed -> reduce { BiometricSetupState(failed = true) }

            is BiometricOutcome.Succeeded -> {
                // Gone only when the process was killed - then there is nothing to encrypt; skipping is left
                val password = draft.password ?: return reduce { BiometricSetupState(failed = true) }
                try {
                    biometrics.save(dbKey, password, outcome.session)
                    toVault()
                } catch (failure: BiometricFailure) {
                    log.unexpected(TAG, failure)
                    reduce { BiometricSetupState(failed = true) }
                }
            }
        }
    }

    private suspend fun toVault() {
        navigator.navigate(MviNavEvent.NavigateTo(EntriesListRoute(dbKey), popUpTo = WelcomeGraph, inclusive = true))
    }

    private companion object {
        const val TAG = "BiometricSetupProcessor"
    }
}
