package ru.kino.dev.welcome.pres.welcome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute
import ru.kino.dev.welcome.pres.navigation.UnlockRoute
import ru.kino.dev.welcome.pres.recordNavigation

class WelcomeProcessorTest {

    @Test
    fun `open asks for the picker of an existing file`() = runTest {
        val (navigator, _) = recordNavigation()

        WelcomeProcessor(navigator).testWithInternalState(this) {
            containerHost.onOpen()

            expectSideEffect(WelcomeSideEffect.PickDatabase)
        }
    }

    @Test
    fun `a picked file goes to the unlock screen, not remembered`() = runTest {
        val (navigator, events) = recordNavigation()

        WelcomeProcessor(navigator).testWithInternalState(this) {
            containerHost.onDatabasePicked(DB)
        }

        assertEquals(listOf(MviNavEvent.NavigateTo(UnlockRoute(pickedUri = DB))), events)
    }

    @Test
    fun `create goes to the master password`() = runTest {
        val (navigator, events) = recordNavigation()

        WelcomeProcessor(navigator).testWithInternalState(this) {
            containerHost.onCreate()
        }

        assertEquals(listOf(MviNavEvent.NavigateTo(MasterPasswordRoute)), events)
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
