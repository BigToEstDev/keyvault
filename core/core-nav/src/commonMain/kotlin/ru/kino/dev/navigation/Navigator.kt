package ru.kino.dev.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Where a processor sends its navigation. The app has one - [AppNavigator]; a container of several screens gives each
 * of them its own, to take what is its to handle (pass-docs, `docs/dev/navigation.md`).
 */
interface Navigator {
    suspend fun navigate(event: MviNavEvent)
}

/** The navigator of the app: the screens send to it, `AppNavHost` listens. */
class AppNavigator : Navigator {

    private val _events = MutableSharedFlow<MviNavEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MviNavEvent> = _events

    override suspend fun navigate(event: MviNavEvent) {
        _events.emit(event)
    }
}

/**
 * The navigator of the screens below: [AppNavigator] at the root of the app, a container's own inside it. A screen
 * hands it to its processor as it creates it.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator: the app provides one at its root") }
