package ru.kino.dev.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class Navigator {

    private val _events = MutableSharedFlow<MviNavEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MviNavEvent> = _events

    suspend fun navigate(event: MviNavEvent) {
        _events.emit(event)
    }
}
