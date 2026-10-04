package ru.kino.dev.welcome.pres.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.kino.dev.welcome.CurrentDatabaseRepository
import javax.inject.Inject

/** Where the app starts: nothing remembered - the welcome screen, a database remembered - unlocking it. */
enum class WelcomeStart {
    Welcome,
    Unlock,
}

/**
 * Decides [WelcomeStart] once, from the remembered database - not from the core, which holds nothing after
 * the process was killed.
 *
 * Not a screen: the activity keeps the system splash on until [start] has a value, so there is no screen of
 * our own to wait on.
 */
@HiltViewModel
class WelcomeStartResolver @Inject constructor(
    private val current: CurrentDatabaseRepository,
) : ViewModel() {

    private val _start = MutableStateFlow<WelcomeStart?>(null)

    /** Null while the remembered database is being read. */
    val start: StateFlow<WelcomeStart?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            _start.value = if (current.current.first() == null) WelcomeStart.Welcome else WelcomeStart.Unlock
        }
    }
}
