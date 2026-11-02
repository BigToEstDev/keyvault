package ru.kino.dev.welcome.pres.welcome

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
import ru.kino.dev.welcome.pres.navigation.UnlockRoute

/**
 * The first screen: open an existing database or create a new one.
 *
 * A picked file is not remembered here - it goes to the unlock screen and is remembered only once it opens.
 *
 * The screen is laid out as pass-docs `docs/app/mvi-arc.md` says (Step 95): the actions are [WelcomeActions], done
 * here as methods.
 */
class WelcomeProcessor(
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<WelcomeState, WelcomeState, WelcomeSideEffect>, WelcomeActions {

    override val container = orbitContainer<WelcomeState, WelcomeSideEffect>(WelcomeState)

    override fun onOpen() {
        intent { postSideEffect(WelcomeSideEffect.PickDatabase) }
    }

    override fun onCreate() {
        intent { navigator.navigate(MviNavEvent.NavigateTo(MasterPasswordRoute)) }
    }

    override fun onDatabasePicked(uri: String) {
        intent { navigator.navigate(MviNavEvent.NavigateTo(UnlockRoute(pickedUri = uri))) }
    }
}
