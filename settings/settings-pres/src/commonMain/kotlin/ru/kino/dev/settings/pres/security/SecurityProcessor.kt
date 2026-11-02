package ru.kino.dev.settings.pres.security

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.biometric.BiometricFailure
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.vault.security.SecurityEditor
import ru.kino.dev.vault.security.SecurityFailure
import ru.kino.dev.vault.security.SecurityRepository

/**
 * The security of the database [dbKey] (pass-docs, Step 67): the credentials, the cipher and the key derivation.
 * Kept in the file, as `Файл базы`: a change is made in the database and the file written at once, with progress;
 * not written - the change is made all the same and waits for the next save.
 *
 * The open time is measured once, when the screen is read - one pass with the memory and the lanes of the file,
 * which the screen does not change - and is that pass times the rounds.
 *
 * A new master password forgets the biometrics of the database - [SecurityEditor] does; the screen says so when
 * they were on, to be turned on again in `Блокировка и вход`.
 */
class SecurityProcessor(
    private val dbKey: String,
    private val repository: SecurityRepository,
    private val editor: SecurityEditor,
    private val biometrics: BiometricRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<SecurityState, SecurityState, Nothing>, SecurityActions {

    override val container = orbitContainer<SecurityState, Nothing>(SecurityState())

    override fun onCurrentPasswordChange(password: String) {
        textIntent {
            reduce { state.editPassword { it.copy(current = password, wrongCurrent = false) } }
        }
    }

    override fun onNewPasswordChange(password: String) {
        textIntent {
            reduce {
                state.editPassword {
                    if (CredentialLimits.isPasswordTooLong(password)) {
                        it.copy(tooLong = true)
                    } else {
                        it.copy(password = password, tooLong = false)
                    }
                }
            }
        }
    }

    override fun onConfirmPasswordChange(password: String) {
        textIntent {
            reduce {
                state.editPassword {
                    if (CredentialLimits.isPasswordTooLong(password)) it else it.copy(confirm = password)
                }
            }
        }
    }

    override fun onLoad() {
        intent { load() }
    }

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onSelectCipher(cipher: Cipher) {
        intent { changeEncryption(SecurityPart.Cipher) { it.copy(cipher = cipher) } }
    }

    override fun onSelectKdf(kdf: KdfVariant) {
        intent { changeEncryption(SecurityPart.Kdf) { it.copy(kdf = kdf) } }
    }

    override fun onChooseRounds() {
        intent {
            if (state.security != null && state.saving == null) {
                reduce { state.copy(choosingRounds = true) }
            }
        }
    }

    override fun onSelectRounds(rounds: Long) {
        intent {
            reduce { state.copy(choosingRounds = false) }
            // The wheel opens on ours for rounds of a database from elsewhere: saving where it opened is a change
            changeEncryption(SecurityPart.Rounds) { it.copy(iterations = rounds) }
        }
    }

    override fun onDismissRounds() {
        intent { reduce { state.copy(choosingRounds = false) } }
    }

    override fun onEditPassword() {
        intent {
            state.security?.let { security ->
                if (state.saving == null) {
                    reduce { state.copy(passwordDialog = PasswordDialog(needsCurrent = security.credentials.password)) }
                }
            }
        }
    }

    override fun onConfirmPasswordLeft() {
        intent { reduce { state.editPassword { it.copy(confirmLeft = true) } } }
    }

    override fun onTogglePasswordVisible(field: PasswordInput) {
        intent {
            reduce {
                state.editPassword {
                    it.copy(visible = if (field in it.visible) it.visible - field else it.visible + field)
                }
            }
        }
    }

    override fun onSavePassword() {
        intent { savePassword() }
    }

    override fun onDismissPassword() {
        intent {
            if (state.passwordDialog?.saving == false) {
                reduce { state.copy(passwordDialog = null) }
            }
        }
    }

    override fun onKeyFilePicked(uri: String) {
        intent {
            changeKeyFile(SecurityMessage.KeyFileSaved) {
                editor.setKeyFile(dbKey, uri)
            }
        }
    }

    override fun onRemoveKeyFile() {
        // Only from a database with a password: the screen offers it no other
        intent {
            if (state.security?.credentials?.password == true && state.saving == null) {
                reduce { state.copy(removingKeyFile = true) }
            }
        }
    }

    override fun onRemoveKeyFileConfirm() {
        intent {
            if (state.removingKeyFile) {
                reduce { state.copy(removingKeyFile = false) }
                changeKeyFile(SecurityMessage.KeyFileRemoved) { editor.removeKeyFile(dbKey) }
            }
        }
    }

    override fun onRemoveKeyFileDismiss() {
        intent { reduce { state.copy(removingKeyFile = false) } }
    }

    override fun onMessageShown() {
        intent { reduce { state.copy(message = null) } }
    }

    /**
     * The key file [change]d, with progress on its row; made - the security read again for its name, since the
     * core keeps the name of a file as the provider gives it.
     */
    private suspend fun Syntax<SecurityState, Nothing>.changeKeyFile(done: SecurityMessage, change: suspend () -> Unit) {
        if (state.security == null || state.saving != null) return

        reduce { state.copy(saving = SecurityPart.KeyFile) }
        val message = try {
            change()
            done
        } catch (_: SecurityFailure.KeyFileTooLarge) {
            SecurityMessage.KeyFileTooLarge
        } catch (failure: SecurityFailure.KeyFileUnreadable) {
            log.unexpected(TAG, failure)
            SecurityMessage.KeyFileUnreadable
        } catch (_: SecurityFailure.NotSaved) {
            // The database has it: it is written with the next save
            SecurityMessage.NotSaved
        } catch (failure: SecurityFailure.KeyFileNotRemembered) {
            log.unexpected(TAG, failure)
            SecurityMessage.KeyFileNotRemembered
        } catch (failure: SecurityFailure) {
            log.unexpected(TAG, failure)
            SecurityMessage.Failed
        }
        val security = try {
            repository.read(dbKey)
        } catch (failure: SecurityFailure) {
            log.unexpected(TAG, failure)
            state.security
        }
        reduce { state.copy(security = security, saving = null, message = message) }
    }

    private suspend fun Syntax<SecurityState, Nothing>.savePassword() {
        val dialog = state.passwordDialog ?: return
        if (!dialog.canSave) return

        // Asked before: the change forgets them. Not known - only the hint to turn them on again is missing
        val biometricsOn = try {
            biometrics.isEnabled(dbKey)
        } catch (failure: BiometricFailure) {
            log.unexpected(TAG, failure)
            false
        }
        reduce { state.editPassword { it.copy(saving = true) } }
        try {
            editor.setPassword(dbKey, current = dialog.current.takeIf { dialog.needsCurrent }, new = dialog.password)
            passwordSet(if (biometricsOn) SecurityMessage.PasswordSavedBiometricsOff else SecurityMessage.PasswordSaved)
        } catch (_: SecurityFailure.WrongPassword) {
            reduce { state.editPassword { it.copy(saving = false, wrongCurrent = true) } }
        } catch (_: SecurityFailure.NotSaved) {
            // The database has it: it is written with the next save
            passwordSet(SecurityMessage.NotSaved)
        } catch (failure: SecurityFailure) {
            reduce { state.editPassword { it.copy(saving = false) } }
            failed(failure)
        }
    }

    private suspend fun Syntax<SecurityState, Nothing>.passwordSet(message: SecurityMessage) {
        reduce {
            state.copy(
                security = state.security?.let { it.copy(credentials = it.credentials.copy(password = true)) },
                passwordDialog = null,
                message = message,
            )
        }
    }

    /** The open dialog of the master password changed by [change]; none open - as it is. */
    private fun SecurityState.editPassword(change: (PasswordDialog) -> PasswordDialog): SecurityState =
        passwordDialog?.let { copy(passwordDialog = change(it)) } ?: this

    private suspend fun Syntax<SecurityState, Nothing>.load() {
        try {
            val security = repository.read(dbKey)
            reduce { state.copy(security = security) }
        } catch (failure: SecurityFailure) {
            failed(failure)
            return
        }
        val pass = try {
            KdfPass.Measured(repository.measureKdfPass(dbKey))
        } catch (failure: SecurityFailure) {
            // Only the time is missing: the screen works without it
            log.unexpected(TAG, failure)
            KdfPass.Unknown
        }
        reduce { state.copy(kdfPass = pass) }
    }

    /** The encryption [change]d shown at once, while the file is written; refused - as it was again. */
    private suspend fun Syntax<SecurityState, Nothing>.changeEncryption(
        part: SecurityPart,
        change: (Encryption) -> Encryption,
    ) {
        val before = state.security ?: return
        val encryption = change(before.encryption)
        if (state.saving != null || encryption == before.encryption) return

        reduce { state.copy(security = before.copy(encryption = encryption), saving = part) }
        try {
            editor.setEncryption(dbKey, encryption)
            reduce { state.copy(saving = null) }
        } catch (_: SecurityFailure.NotSaved) {
            // The database has it: it is written with the next save
            reduce { state.copy(saving = null, message = SecurityMessage.NotSaved) }
        } catch (failure: SecurityFailure) {
            reduce { state.copy(security = before, saving = null) }
            failed(failure)
        }
    }

    private suspend fun Syntax<SecurityState, Nothing>.failed(failure: SecurityFailure) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = SecurityMessage.Failed) }
    }

    private companion object {
        const val TAG = "SecurityProcessor"
    }
}
