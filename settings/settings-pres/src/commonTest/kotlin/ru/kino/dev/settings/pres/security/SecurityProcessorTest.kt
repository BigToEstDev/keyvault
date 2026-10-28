package ru.kino.dev.settings.pres.security

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.CredentialsUsed
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
    private val journal = mutableListOf<String>()
    private val biometrics = FakeBiometrics(journal)
    private val remembered = FakeRememberedKeyFile(journal)

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

    // A database from elsewhere with more rounds than the wheel offers: shown as its own 20, the wheel opens on 10
    @Test
    fun `rounds outside the wheel stay in the file until a choice is saved`() = runTest {
        security.security = security.security.copy(encryption = security.security.encryption.copy(iterations = 20))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.SelectCipher(Cipher.ChaCha20))

        assertEquals(20L, security.security.encryption.iterations)
        assertEquals(20L, state.security?.encryption?.iterations)
        assertEquals(RoundsChoices.MAX, RoundsChoices.onWheel(20))
    }

    @Test
    fun `the shown rounds of a database from elsewhere saved on the wheel are a change`() = runTest {
        security.security = security.security.copy(encryption = security.security.encryption.copy(iterations = 20))

        perform(SecurityAction.Load, SecurityAction.SelectRounds(RoundsChoices.MAX))

        assertEquals(RoundsChoices.MAX, security.security.encryption.iterations)
        assertEquals(listOf("save"), database.calls)
    }

    // --- Master password ---

    @Test
    fun `the password tapped asks the current one of a database with a password`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.EditPassword)

        assertEquals(PasswordDialog(needsCurrent = true), state.passwordDialog)
    }

    @Test
    fun `a database with only a key file gets the first password without a current one`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = false, keyFile = true))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.EditPassword)

        assertEquals(PasswordDialog(needsCurrent = false), state.passwordDialog)
    }

    @Test
    fun `a new password over the limit is not taken`() = runTest {
        val tooLong = "a".repeat(CredentialLimits.PASSWORD_MAX_CHARS + 1)

        val (state, _) = perform(SecurityAction.Load, SecurityAction.EditPassword, SecurityAction.NewPasswordChanged(tooLong))

        assertEquals("", state.passwordDialog?.password)
        assertEquals(true, state.passwordDialog?.tooLong)
    }

    @Test
    fun `a short password cannot be saved`() = runTest {
        val (state, _) = perform(
            SecurityAction.Load,
            SecurityAction.EditPassword,
            SecurityAction.CurrentPasswordChanged(CURRENT),
            SecurityAction.NewPasswordChanged("short"),
            SecurityAction.ConfirmPasswordChanged("short"),
            SecurityAction.SavePassword,
        )

        assertEquals(false, state.passwordDialog?.canSave)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a new password is saved, the dialog closes and the row says set`() = runTest {
        val (state, _) = perform(*typed(CURRENT), SecurityAction.SavePassword)

        assertNull(state.passwordDialog)
        assertEquals(SecurityMessage.PasswordSaved, state.message)
        assertEquals(NEW, security.password)
        assertEquals(listOf("save"), database.calls)
    }

    @Test
    fun `a new password with biometrics on says they are off now`() = runTest {
        biometrics.passwords[DB] = CURRENT

        val (state, _) = perform(*typed(CURRENT), SecurityAction.SavePassword)

        assertEquals(SecurityMessage.PasswordSavedBiometricsOff, state.message)
        assertEquals(listOf("forget $DB"), journal)
    }

    @Test
    fun `a wrong current password keeps the dialog and says so`() = runTest {
        val (state, _) = perform(*typed("not it"), SecurityAction.SavePassword)

        assertEquals(true, state.passwordDialog?.wrongCurrent)
        assertEquals(false, state.passwordDialog?.saving)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `the current password typed again takes the wrong one away`() = runTest {
        val (state, _) = perform(*typed("not it"), SecurityAction.SavePassword, SecurityAction.CurrentPasswordChanged(CURRENT))

        assertEquals(false, state.passwordDialog?.wrongCurrent)
    }

    @Test
    fun `a new password not written closes the dialog and says not saved`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, _) = perform(*typed(CURRENT), SecurityAction.SavePassword)

        assertNull(state.passwordDialog)
        assertEquals(SecurityMessage.NotSaved, state.message)
    }

    @Test
    fun `the eye of a field shows that one alone`() = runTest {
        val (state, _) = perform(
            SecurityAction.Load,
            SecurityAction.EditPassword,
            SecurityAction.TogglePasswordVisible(PasswordInput.New),
            SecurityAction.TogglePasswordVisible(PasswordInput.Confirm),
            SecurityAction.TogglePasswordVisible(PasswordInput.Confirm),
        )

        assertEquals(setOf(PasswordInput.New), state.passwordDialog?.visible)
    }

    @Test
    fun `the dialog dismissed changes nothing`() = runTest {
        val (state, _) = perform(*typed(CURRENT), SecurityAction.DismissPassword)

        assertNull(state.passwordDialog)
        assertEquals(emptyList<String>(), database.calls)
    }

    // --- Key file ---

    @Test
    fun `a key file picked is set, written and its name shown`() = runTest {
        val (state, _) = perform(SecurityAction.Load, SecurityAction.KeyFilePicked(KEY))

        assertEquals("home.keyx", state.security?.keyFileName)
        assertEquals(true, state.security?.credentials?.keyFile)
        assertNull(state.saving)
        assertEquals(SecurityMessage.KeyFileSaved, state.message)
        assertEquals(listOf("remember $DB $KEY"), journal)
    }

    @Test
    fun `a key file over the limit says so and changes nothing`() = runTest {
        security.keyFileFailure = SecurityFailure.KeyFileTooLarge()

        val (state, _) = perform(SecurityAction.Load, SecurityAction.KeyFilePicked(KEY))

        assertNull(state.security?.keyFileName)
        assertEquals(SecurityMessage.KeyFileTooLarge, state.message)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `a key file that cannot be read says so`() = runTest {
        security.keyFileFailure = SecurityFailure.KeyFileUnreadable()

        val (state, _) = perform(SecurityAction.Load, SecurityAction.KeyFilePicked(KEY))

        assertEquals(SecurityMessage.KeyFileUnreadable, state.message)
    }

    @Test
    fun `a key file saved but not remembered says to pick it at sign-in`() = runTest {
        remembered.failure = IOException("store gone")

        val (state, _) = perform(SecurityAction.Load, SecurityAction.KeyFilePicked(KEY))

        assertEquals("home.keyx", state.security?.keyFileName)
        assertEquals(SecurityMessage.KeyFileNotRemembered, state.message)
    }

    @Test
    fun `remove asks first`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = true, keyFile = true))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.RemoveKeyFile)

        assertTrue(state.removingKeyFile)
        assertEquals(emptyList<String>(), database.calls)
    }

    @Test
    fun `remove confirmed takes the key file away`() = runTest {
        security.security = security.security.copy(
            credentials = CredentialsUsed(password = true, keyFile = true),
            keyFileName = "home.keyx",
        )

        val (state, _) = perform(SecurityAction.Load, SecurityAction.RemoveKeyFile, SecurityAction.RemoveKeyFileConfirm)

        assertFalse(state.removingKeyFile)
        assertNull(state.security?.keyFileName)
        assertEquals(false, state.security?.credentials?.keyFile)
        assertEquals(SecurityMessage.KeyFileRemoved, state.message)
        assertEquals(listOf("remember $DB null"), journal)
    }

    @Test
    fun `the only key file is not offered to remove`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = false, keyFile = true))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.RemoveKeyFile)

        assertFalse(state.removingKeyFile)
    }

    @Test
    fun `remove dismissed changes nothing`() = runTest {
        security.security = security.security.copy(credentials = CredentialsUsed(password = true, keyFile = true))

        val (state, _) = perform(SecurityAction.Load, SecurityAction.RemoveKeyFile, SecurityAction.RemoveKeyFileDismiss)

        assertFalse(state.removingKeyFile)
        assertEquals(emptyList<String>(), database.calls)
    }

    /** The screen read, the dialog open, [current] and a new password typed twice. */
    private fun typed(current: String): Array<Any> = arrayOf(
        SecurityAction.Load,
        SecurityAction.EditPassword,
        SecurityAction.CurrentPasswordChanged(current),
        SecurityAction.NewPasswordChanged(NEW),
        SecurityAction.ConfirmPasswordChanged(NEW),
    )

    @Test
    fun `back goes back`() = runTest {
        val (_, events) = perform(SecurityAction.Back)

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    private suspend fun TestScope.perform(vararg steps: Any): Pair<SecurityState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val editor = SecurityEditor(security, database, biometrics, remembered)
        val processor = SecurityProcessor(DB, security, editor, biometrics, navigator, log)

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
        const val CURRENT = "current password"
        const val NEW = "new password 12"
        const val KEY = "content://keys/home.keyx"
    }
}
