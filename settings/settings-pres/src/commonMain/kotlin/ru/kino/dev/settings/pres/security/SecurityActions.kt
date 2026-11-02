package ru.kino.dev.settings.pres.security

import ru.kino.dev.core.Cipher
import ru.kino.dev.core.KdfVariant

/** What the security of the database is asked to do - done by [SecurityProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface SecurityActions {

    /** The screen is shown: the security is read, then one KDF pass is measured. */
    fun onLoad()

    fun onBack()

    /** A cipher chosen: kept and the file written at once. */
    fun onSelectCipher(cipher: Cipher)

    /** An Argon2 variant chosen: kept and the file written at once. */
    fun onSelectKdf(kdf: KdfVariant)

    /** The rounds tapped: the wheel with them. */
    fun onChooseRounds()

    /** "Save" on the wheel of the rounds: kept and the file written. */
    fun onSelectRounds(rounds: Long)

    fun onDismissRounds()

    /** The master password tapped: the dialog - to change it, or to set the first one of a database without. */
    fun onEditPassword()

    fun onCurrentPasswordChange(password: String)

    fun onNewPasswordChange(password: String)

    fun onConfirmPasswordChange(password: String)

    /** The confirmation lost focus: a mismatch is shown from now on. */
    fun onConfirmPasswordLeft()

    /** The eye of one field: shows or hides that one alone. */
    fun onTogglePasswordVisible(field: PasswordInput)

    /** "Save" in the dialog: the current one checked, the new one set and the file written. */
    fun onSavePassword()

    fun onDismissPassword()

    /** A key file picked - a first one, or another: read, set and the file written at once. */
    fun onKeyFilePicked(uri: String)

    /** "Remove" of the key file: asked first. */
    fun onRemoveKeyFile()

    fun onRemoveKeyFileConfirm()

    fun onRemoveKeyFileDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
