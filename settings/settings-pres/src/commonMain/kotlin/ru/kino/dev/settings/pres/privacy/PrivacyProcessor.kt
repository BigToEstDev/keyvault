package ru.kino.dev.settings.pres.privacy

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.settings.privacy.PrivacySettingsRepository

/**
 * What the app lets out of its screen (pass-docs, Step 61): screenshots, recording, casting, the preview in the
 * recent apps - one switch, Android does not tell them apart. A choice is kept at once - the activity takes it from
 * the repository and lifts or puts the flag of the window without a restart.
 */
class PrivacyProcessor(
    private val repository: PrivacySettingsRepository,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<PrivacyState, PrivacyState, Nothing>, PrivacyActions {

    override val container = orbitContainer<PrivacyState, Nothing>(PrivacyState())

    override fun onLoad() {
        intent {
            val allowScreenshots = repository.allowScreenshots.first()
            reduce { state.copy(allowScreenshots = allowScreenshots) }
        }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onSetAllowScreenshots(on: Boolean) {
        intent {
            keepSetting(
                chosen = on,
                current = state.allowScreenshots,
                kept = repository.allowScreenshots,
                show = { copy(allowScreenshots = it) },
                notSaved = { copy(message = PrivacyMessage.NotSaved) },
            ) { repository.saveAllowScreenshots(it) }
        }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }
}
