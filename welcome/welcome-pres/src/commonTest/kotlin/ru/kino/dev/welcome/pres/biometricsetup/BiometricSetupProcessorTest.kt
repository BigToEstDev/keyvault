package ru.kino.dev.welcome.pres.biometricsetup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.welcome.pres.FakeErrorLog
import ru.kino.dev.welcome.pres.common.BiometricOutcome
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.recordNavigation

class BiometricSetupProcessorTest {

    private val journal = mutableListOf<String>()
    private val biometrics = FakeBiometrics(journal)
    private val draft = CreateDraft().apply { start("pw", withKeyFile = false) }

    private fun TestScope.processor() =
        recordNavigation().let { (navigator, events) ->
            BiometricSetupProcessor(DB, draft, biometrics, navigator, FakeErrorLog()) to events
        }

    @Test
    fun `turning on encrypts the password of the draft and goes to vault - not asked again`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this) {
            containerHost.onEnable()
            expectInternalState(BiometricSetupState(busy = true))
            val prompt = awaitSideEffect() as BiometricSetupSideEffect.ShowPrompt

            containerHost.onPromptFinished(BiometricOutcome.Succeeded(prompt.session))
        }

        assertEquals(listOf("startSaving", "save $DB"), journal)
        assertEquals("pw", biometrics.passwords[DB])
        assertEquals(listOf(toVault()), events)
    }

    @Test
    fun `a cancelled prompt stays here, ready to try again or skip`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, BiometricSetupState(busy = true)) {
            containerHost.onPromptFinished(BiometricOutcome.Cancelled)
            expectInternalState(BiometricSetupState())
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a failed prompt says so, and skipping still works`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this, BiometricSetupState(busy = true)) {
            containerHost.onPromptFinished(BiometricOutcome.Failed)
            expectInternalState(BiometricSetupState(failed = true))

            containerHost.onSkip()
        }

        assertEquals(listOf(toVault()), events)
    }

    @Test
    fun `skipping goes to vault and wipes the welcome graph - the database is open already`() = runTest {
        val (processor, events) = processor()

        processor.testWithInternalState(this) {
            containerHost.onSkip()
        }

        assertTrue(journal.isEmpty())
        assertEquals(listOf(toVault()), events)
    }

    private fun toVault() = MviNavEvent.NavigateTo(VaultRootRoute(DB), popUpTo = WelcomeGraph, inclusive = true)

    private companion object {
        const val DB = "content://docs/kdbxvault.kdbx"
    }
}
