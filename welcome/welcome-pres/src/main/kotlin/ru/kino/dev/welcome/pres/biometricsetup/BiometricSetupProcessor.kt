package ru.kino.dev.welcome.pres.biometricsetup

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.WelcomeGraph

/** Nothing changes on the screen until enabling biometrics arrives (Step 33). */
data object BiometricSetupState

/**
 * The last step of creating: offering biometrics. The database is created and open by now, so whatever
 * the answer, the next stop is `vault` - the password is not asked again.
 *
 * Only "skip" for now; enabling it - Android Keystore and the prompt - is Step 33.
 */
@HiltViewModel(assistedFactory = BiometricSetupProcessor.Factory::class)
class BiometricSetupProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<BiometricSetupState, BiometricSetupState, BiometricSetupSideEffect> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): BiometricSetupProcessor
    }

    override val container = orbitContainer<BiometricSetupState, BiometricSetupSideEffect>(BiometricSetupState)

    fun action(action: BiometricSetupAction) = intent {
        when (action) {
            BiometricSetupAction.Skip -> navigator.navigate(
                MviNavEvent.NavigateTo(EntriesListRoute(dbKey), popUpTo = WelcomeGraph, inclusive = true),
            )
        }
    }
}
