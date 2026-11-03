package ru.kino.dev.pane

import kotlinx.coroutines.test.runTest
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals

class PaneNavigatorTest {

    private val parent = RecordingNavigator()
    private val taken = mutableListOf<MviNavEvent>()

    // The container takes "back" - closes the pane; the rest is not its
    private val navigator = PaneNavigator(parent) { event ->
        (event == MviNavEvent.NavigateBack).also { if (it) taken += event }
    }

    @Test
    fun `what the container takes stays in it`() = runTest {
        navigator.navigate(MviNavEvent.NavigateBack)

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), taken)
        assertEquals(emptyList(), parent.events)
    }

    @Test
    fun `what it does not take goes on`() = runTest {
        val open = MviNavEvent.NavigateTo(route = "settings")

        navigator.navigate(open)

        assertEquals(emptyList(), taken)
        assertEquals(listOf<MviNavEvent>(open), parent.events)
    }

    private class RecordingNavigator : Navigator {
        val events = mutableListOf<MviNavEvent>()

        override suspend fun navigate(event: MviNavEvent) {
            events += event
        }
    }
}
