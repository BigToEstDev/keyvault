package ru.kino.dev.settings.pres.security

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.Encryption
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
 * @param security the security of the database; null until read - the screen is a skeleton meanwhile
 * @param kdfPass one KDF pass on this phone, for the open time
 * @param choosingRounds the wheel of the rounds, while open
 * @param passwordDialog the dialog of the master password, while open
 * @param removingKeyFile "remove the key file?", while asked
 * @param saving the part written into the file - every row waits, that one shows progress
 * @param message for the snackbar, until it takes it
 */
data class SecurityState(
    val security: DatabaseSecurity? = null,
    val kdfPass: KdfPass = KdfPass.Measuring,
    val choosingRounds: Boolean = false,
    val passwordDialog: PasswordDialog? = null,
    val removingKeyFile: Boolean = false,
    val saving: SecurityPart? = null,
    val message: SecurityMessage? = null,
)

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
) : ViewModel(), OrbitContainerHost<SecurityState, SecurityState, Nothing> {

    override val container = orbitContainer<SecurityState, Nothing>(SecurityState())

    fun action(action: SecurityAction): Job = when (action) {
        is SecurityAction.CurrentPasswordChanged -> textIntent {
            reduce { state.editPassword { it.copy(current = action.password, wrongCurrent = false) } }
        }

        is SecurityAction.NewPasswordChanged -> textIntent {
            reduce {
                state.editPassword {
                    if (CredentialLimits.isPasswordTooLong(action.password)) {
                        it.copy(tooLong = true)
                    } else {
                        it.copy(password = action.password, tooLong = false)
                    }
                }
            }
        }

        is SecurityAction.ConfirmPasswordChanged -> textIntent {
            reduce {
                state.editPassword {
                    if (CredentialLimits.isPasswordTooLong(action.password)) it else it.copy(confirm = action.password)
                }
            }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<SecurityState, Nothing>.handle(action: SecurityAction) {
        when (action) {
            is SecurityAction.CurrentPasswordChanged,
            is SecurityAction.NewPasswordChanged,
            is SecurityAction.ConfirmPasswordChanged,
            -> Unit // in action()

            SecurityAction.Load -> load()

            SecurityAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is SecurityAction.SelectCipher -> changeEncryption(SecurityPart.Cipher) { it.copy(cipher = action.cipher) }

            is SecurityAction.SelectKdf -> changeEncryption(SecurityPart.Kdf) { it.copy(kdf = action.kdf) }

            SecurityAction.ChooseRounds -> if (state.security != null && state.saving == null) {
                reduce { state.copy(choosingRounds = true) }
            }

            is SecurityAction.SelectRounds -> {
                reduce { state.copy(choosingRounds = false) }
                // The wheel opens on ours for rounds of a database from elsewhere: saving where it opened is a change
                changeEncryption(SecurityPart.Rounds) { it.copy(iterations = action.rounds) }
            }

            SecurityAction.DismissRounds -> reduce { state.copy(choosingRounds = false) }

            SecurityAction.EditPassword -> state.security?.let { security ->
                if (state.saving == null) {
                    reduce { state.copy(passwordDialog = PasswordDialog(needsCurrent = security.credentials.password)) }
                }
            }

            SecurityAction.ConfirmPasswordLeft -> reduce { state.editPassword { it.copy(confirmLeft = true) } }

            is SecurityAction.TogglePasswordVisible -> reduce {
                state.editPassword {
                    it.copy(visible = if (action.field in it.visible) it.visible - action.field else it.visible + action.field)
                }
            }

            SecurityAction.SavePassword -> savePassword()

            SecurityAction.DismissPassword -> if (state.passwordDialog?.saving == false) {
                reduce { state.copy(passwordDialog = null) }
            }

            is SecurityAction.KeyFilePicked -> changeKeyFile(SecurityMessage.KeyFileSaved) {
                editor.setKeyFile(dbKey, action.uri)
            }

            // Only from a database with a password: the screen offers it no other
            SecurityAction.RemoveKeyFile -> if (state.security?.credentials?.password == true && state.saving == null) {
                reduce { state.copy(removingKeyFile = true) }
            }

            SecurityAction.RemoveKeyFileConfirm -> if (state.removingKeyFile) {
                reduce { state.copy(removingKeyFile = false) }
                changeKeyFile(SecurityMessage.KeyFileRemoved) { editor.removeKeyFile(dbKey) }
            }

            SecurityAction.RemoveKeyFileDismiss -> reduce { state.copy(removingKeyFile = false) }

            SecurityAction.MessageShown -> reduce { state.copy(message = null) }
        }
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
