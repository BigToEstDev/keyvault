package ru.kino.dev.welcome.pres.welcome

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
import ru.kino.dev.welcome.pres.navigation.UnlockRoute

/** The welcome screen has nothing to show that changes. */
data object WelcomeState

/**
 * The first screen: open an existing database or create a new one.
 *
 * A picked file is not remembered here - it goes to the unlock screen and is remembered only once it opens.
 */
class WelcomeProcessor(
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<WelcomeState, WelcomeState, WelcomeSideEffect> {

    override val container = orbitContainer<WelcomeState, WelcomeSideEffect>(WelcomeState)

    fun action(action: WelcomeAction) = intent {
        when (action) {
            WelcomeAction.OpenClicked -> postSideEffect(WelcomeSideEffect.PickDatabase)

            is WelcomeAction.DatabasePicked ->
                navigator.navigate(MviNavEvent.NavigateTo(UnlockRoute(pickedUri = action.uri)))

            WelcomeAction.CreateClicked -> navigator.navigate(MviNavEvent.NavigateTo(MasterPasswordRoute))
        }
    }
}
