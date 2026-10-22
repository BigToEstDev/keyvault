package ru.kino.dev.settings.pres.security

import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.biometric.FakeBiometrics
import ru.kino.dev.settings.pres.testing.RecordingLog
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.settings.signin.FakeRememberedKeyFile
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.security.FakeSecurityRepository
import ru.kino.dev.vault.security.SecurityEditor
import ru.kino.dev.vault.security.SecurityFailure

class SecurityProcessorTest {

    private val security = FakeSecurityRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()

    @Test
    fun `the screen shown reads the security and measures a pass`() = runTest {
        security.kdfPass = 900.milliseconds

        val (state, _) = perform(SecurityAction.Load)

        assertEquals(security.security, state.security)
        assertEquals(KdfPass.Measured(900.milliseconds), state.kdfPass)
    }

    @Test
    fun `security that cannot be read says so and goes to the log`() = runTest {
        security.failure = SecurityFailure.Unexpected()

        val (state, _) = perform(SecurityAction.Load)

        assertNull(state.security)
        assertEquals(SecurityMessage.Failed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `a pass that cannot be measured leaves the screen without the time`() = runTest {
        security.measureFailure = SecurityFailure.Unexpected()

        val (state, _) = perform(SecurityAction.Load)

        assertEquals(security.security, state.security)
        assertEquals(KdfPass.Unknown, state.kdfPass)
        assertNull(state.message)
    }

    @Test
    fun `a cipher chosen is written at once`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.SelectCipher(Cipher.ChaCha20))

        assertEquals(Cipher.ChaCha20, state.security?.encryption?.cipher)
        assertEquals(Cipher.ChaCha20, security.security.encryption.cipher)
        assertNull(state.saving)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `the cipher there is chosen again writes nothing`() = runTest {
        perform(SecurityAction.Load, SecurityAction.SelectCipher(Cipher.Aes256))

        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a variant chosen is written at once`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.SelectKdf(KdfVariant.Argon2d))

        assertEquals(KdfVariant.Argon2d, state.security?.encryption?.kdf)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a choice not written stays and says so`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(SecurityAction.Load, SecurityAction.SelectKdf(KdfVariant.Argon2d))

        assertEquals(KdfVariant.Argon2d, state.security?.encryption?.kdf)
        assertEquals(SecurityMessage.NotSaved, state.message)
    }

    @Test
    fun `a choice refused by the core goes back`() = runTest {
        val (state, _) = perform(SecurityAction.Load, FailNext(security), SecurityAction.SelectCipher(Cipher.ChaCha20))

        assertEquals(Cipher.Aes256, state.security?.encryption?.cipher)
        assertEquals(SecurityMessage.Failed, state.message)
    }

    @Test
    fun `the rounds tapped open the wheel`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.ChooseRounds)

        assertTrue(state.choosingRounds)
    }

    @Test
    fun `the rounds saved on the wheel are written and close it`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.ChooseRounds, SecurityAction.SelectRounds(5))

        assertFalse(state.choosingRounds)
        assertEquals(5L, security.security.encryption.iterations)
        assertEquals(listOf("save"), database.calls)
    }

    // A database from elsewhere with more rounds than the wheel offers: shown as 10, its own 20 stay in the file
    @Test
    fun `rounds outside the wheel stay in the file until a choice is saved`() = runTest {
        security.security = security.security.copy(encryption = security.security.encryption.copy(iterations = 20))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.SelectCipher(Cipher.ChaCha20))

        assertEquals(20L, security.security.encryption.iterations)
        assertEquals(RoundsChoices.MAX, RoundsChoices.shown(state.security!!.encryption.iterations))
    }

    @Test
    fun `the shown rounds of a database from elsewhere saved on the wheel are a change`() = runTest {
        security.security = security.security.copy(encryption = security.security.encryption.copy(iterations = 20))

        perform(SecurityAction.Load, SecurityAction.SelectRounds(RoundsChoices.MAX))

        assertEquals(RoundsChoices.MAX, security.security.encryption.iterations)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `back goes back`() = runTest {
        val (_, events) = perform(SecurityAction.Back)

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(vararg steps: Any): Pair<SecurityState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val editor = SecurityEditor(security, database, FakeBiometrics(), FakeRememberedKeyFile())
        val processor = SecurityProcessor(DB, security, editor, navigator, log)

        processor.testWithInternalState(this) {
            steps.forEach { step ->
                when (step) {
                    is SecurityAction -> containerHost.action(step).join()
                    is FailNext -> step.security.failure = SecurityFailure.Unexpected()
                }
            }
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    /** From here on the core refuses - after the screen read what it shows. */
    private class FailNext(val security: FakeSecurityRepository)

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
