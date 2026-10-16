package ru.kino.dev.navigation

sealed interface MviNavEvent {

    data class NavigateTo(
        val route: Any,
        val popUpTo: Any? = null,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = false,
    ) : MviNavEvent

    data object NavigateBack : MviNavEvent

    /**
     * Back to [popUpTo] - and past it, [inclusive]. When it is not on the stack: plain "back" with
     * [orBack], nothing otherwise.
     */
    data class NavigateBackTo(
        val popUpTo: Any,
        val inclusive: Boolean = false,
        val orBack: Boolean = false,
    ) : MviNavEvent

    data object NavigateUp : MviNavEvent
}
