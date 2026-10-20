package ru.kino.dev.settings.pres.lock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.pres.common.keepSetting
import javax.inject.Inject

/**
 * @param timeout the chosen time of the auto-lock; null until read - its choice is disabled
 * @param lockOnLeave whether leaving the app locks at once; null until read - its choice is disabled
 * @param choosingTimeout the wheel of the times is open
 * @param message for the snackbar, until it takes it
 */
data class LockState(
    val timeout: AutoLockTimeout? = null,
    val lockOnLeave: Boolean? = null,
    val choosingTimeout: Boolean = false,
    val message: LockMessage? = null,
)

/**
 * How the database locks (pass-docs, Step 59): the time of the auto-lock, locking on leaving the app. A choice
 * is kept at once - the timer of the app takes it from the repository; a new time counts from then on.
 */
@HiltViewModel
class LockProcessor @Inject constructor(
    private val repository: LockSettingsRepository,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<LockState, LockState, Nothing> {

    override val container = orbitContainer<LockState, Nothing>(LockState())

    fun action(action: LockAction): Job = intent {
        when (action) {
            LockAction.Load -> {
                val timeout = repository.autoLockTimeout.first()
                val lockOnLeave = repository.lockOnLeave.first()
                reduce { state.copy(timeout = timeout, lockOnLeave = lockOnLeave) }
            }

            LockAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            LockAction.ChooseTimeout -> if (state.timeout != null) reduce { state.copy(choosingTimeout = true) }

            LockAction.DismissTimeout -> reduce { state.copy(choosingTimeout = false) }

            is LockAction.SelectTimeout -> {
                reduce { state.copy(choosingTimeout = false) }
                keepSetting(
                    chosen = action.timeout,
                    current = state.timeout,
                    kept = repository.autoLockTimeout,
                    show = { copy(timeout = it) },
                    notSaved = { copy(message = LockMessage.NotSaved) },
                ) { repository.save(it) }
            }

            is LockAction.SetLockOnLeave -> keepSetting(
                chosen = action.on,
                current = state.lockOnLeave,
                kept = repository.lockOnLeave,
                show = { copy(lockOnLeave = it) },
                notSaved = { copy(message = LockMessage.NotSaved) },
            ) { repository.saveLockOnLeave(it) }

            LockAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }
}
