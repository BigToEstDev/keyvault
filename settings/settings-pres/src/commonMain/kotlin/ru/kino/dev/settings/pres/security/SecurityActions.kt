package ru.kino.dev.settings.pres.security

import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.KdfVariant
import kotlin.time.Duration

sealed interface SecurityAction {

    /** The screen is shown: the security is read, then one KDF pass is measured. */
    data object Load : SecurityAction

    data object Back : SecurityAction

    /** A cipher chosen: kept and the file written at once. */
    data class SelectCipher(val cipher: Cipher) : SecurityAction

    /** An Argon2 variant chosen: kept and the file written at once. */
    data class SelectKdf(val kdf: KdfVariant) : SecurityAction

    /** The rounds tapped: the wheel with them. */
    data object ChooseRounds : SecurityAction

    /** "Save" on the wheel of the rounds: kept and the file written. */
    data class SelectRounds(val rounds: Long) : SecurityAction

    data object DismissRounds : SecurityAction

    /** The master password tapped: the dialog - to change it, or to set the first one of a database without. */
    data object EditPassword : SecurityAction

    data class CurrentPasswordChanged(val password: String) : SecurityAction

    data class NewPasswordChanged(val password: String) : SecurityAction

    data class ConfirmPasswordChanged(val password: String) : SecurityAction

    /** The confirmation lost focus: a mismatch is shown from now on. */
    data object ConfirmPasswordLeft : SecurityAction

    /** The eye of one field: shows or hides that one alone. */
    data class TogglePasswordVisible(val field: PasswordInput) : SecurityAction

    /** "Save" in the dialog: the current one checked, the new one set and the file written. */
    data object SavePassword : SecurityAction

    data object DismissPassword : SecurityAction

    /** A key file picked - a first one, or another: read, set and the file written at once. */
    data class KeyFilePicked(val uri: String) : SecurityAction

    /** "Remove" of the key file: asked first. */
    data object RemoveKeyFile : SecurityAction

    data object RemoveKeyFileConfirm : SecurityAction

    data object RemoveKeyFileDismiss : SecurityAction

    /** The snackbar took the message. */
    data object MessageShown : SecurityAction
}

/**
 * The dialog of the master password, as `masterpassword` of creating: 12 to 256 characters, typed twice. Each
 * field has its eye.
 *
 * @param needsCurrent the database has a password: it is asked first. One with only a key file gets its first
 * password without
 * @param visible the fields shown as typed
 * @param confirmLeft the confirmation lost focus once - a mismatch is shown only after that
 * @param tooLong the last thing typed was over the limit and was not taken
 * @param wrongCurrent the current password given did not match - said under it until it is typed again
 * @param saving the password is checked and the file written - the dialog waits, it does not close
 */
data class PasswordDialog(
    val needsCurrent: Boolean,
    val current: String = "",
    val password: String = "",
    val confirm: String = "",
    val visible: Set<PasswordInput> = emptySet(),
    val confirmLeft: Boolean = false,
    val tooLong: Boolean = false,
    val wrongCurrent: Boolean = false,
    val saving: Boolean = false,
) {

    /** Shorter than the minimum - said under the field while it is, and "Save" stays off. */
    val tooShort: Boolean
        get() = CredentialLimits.isPasswordTooShort(password)

    val mismatch: Boolean
        get() = confirmLeft && confirm.isNotEmpty() && confirm != password

    val canSave: Boolean
        get() = (!needsCurrent || current.isNotEmpty()) && !tooShort && password == confirm && !saving

    // Never the passwords: a state in a log is the password given away
    override fun toString(): String = "PasswordDialog(needsCurrent=$needsCurrent, saving=$saving)"
}

/** A field of the dialog of the master password. */
enum class PasswordInput { Current, New, Confirm }

/** What part of the screen is written into the file - every row waits, that one shows progress. */
enum class SecurityPart { Cipher, Kdf, Rounds, KeyFile }

/** One KDF pass on this phone: the open time is a pass times the rounds. */
sealed interface KdfPass {

    /** Measured in the background, after the screen is read - no open time meanwhile, it opens out once known. */
    data object Measuring : KdfPass

    data class Measured(val pass: Duration) : KdfPass

    /** It could not be measured: no time is shown. */
    data object Unknown : KdfPass
}

/** What the snackbar says. */
enum class SecurityMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,

    /** The new master password is written. */
    PasswordSaved,

    /** The same, and the biometrics of the database are off: the stored password opens it no more. */
    PasswordSavedBiometricsOff,

    /** The new key file is written - to be kept apart from the database. */
    KeyFileSaved,

    /** The key file is gone: the password alone opens the database. */
    KeyFileRemoved,

    /** The key file picked is over the limit. Nothing changed. */
    KeyFileTooLarge,

    /** The key file picked cannot be read, or its access kept. Nothing changed. */
    KeyFileUnreadable,

    /** The key file is changed and written, but the sign-in remembers the old one. */
    KeyFileNotRemembered,
}
