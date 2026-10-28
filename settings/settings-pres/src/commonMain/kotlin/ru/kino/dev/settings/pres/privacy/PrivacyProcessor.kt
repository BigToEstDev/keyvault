package ru.kino.dev.settings.pres.privacy

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.pres.common.keepSetting
import ru.kino.dev.settings.privacy.PrivacySettingsRepository

/**
 * @param allowScreenshots whether the screen of the app may be captured; null until read - its choice is disabled
 * @param message for the snackbar, until it takes it
 */
data class PrivacyState(
    val allowScreenshots: Boolean? = null,
    val message: PrivacyMessage? = null,
)

/**
 * What the app lets out of its screen (pass-docs, Step 61): screenshots, recording, casting, the preview in the
 * recent apps - one switch, Android does not tell them apart. A choice is kept at once - the activity takes it from
 * the repository and lifts or puts the flag of the window without a restart.
 */
class PrivacyProcessor(
    private val repository: PrivacySettingsRepository,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<PrivacyState, PrivacyState, Nothing> {

    override val container = orbitContainer<PrivacyState, Nothing>(PrivacyState())

    fun action(action: PrivacyAction): Job = intent {
        when (action) {
            PrivacyAction.Load -> {
                val allowScreenshots = repository.allowScreenshots.first()
                reduce { state.copy(allowScreenshots = allowScreenshots) }
            }

            PrivacyAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is PrivacyAction.SetAllowScreenshots -> keepSetting(
                chosen = action.on,
                current = state.allowScreenshots,
                kept = repository.allowScreenshots,
                show = { copy(allowScreenshots = it) },
                notSaved = { copy(message = PrivacyMessage.NotSaved) },
            ) { repository.saveAllowScreenshots(it) }

            PrivacyAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }
}
