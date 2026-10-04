package ru.kino.dev.welcome.pres.biometricsetup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.EntriesListRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.welcome.pres.recordNavigation

class BiometricSetupProcessorTest {

    @Test
    fun `skipping goes to vault and wipes the welcome graph - the database is open already`() = runTest {
        val (navigator, events) = recordNavigation()

        BiometricSetupProcessor(DB, navigator).testWithInternalState(this) {
            containerHost.action(BiometricSetupAction.Skip).join()
        }

        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntriesListRoute(DB), popUpTo = WelcomeGraph, inclusive = true)),
            events,
        )
    }

    private companion object {
        const val DB = "content://docs/kdbxvault.kdbx"
    }
}
