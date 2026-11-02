package ru.kino.dev.welcome.pres.common.passwordentry

/**
 * What the layout of entering a password is asked to do (pass-docs, Step 94) - done by the processor of the screen
 * itself. What a screen has not got is said by [PasswordEntryAbilities]; the choices only some screens have are
 * [PasswordEntryChoiceActions]. The way of it - pass-docs `docs/app/mvi-arc.md`.
 */
interface PasswordEntryActions : PasswordEntryChoiceActions {

    fun onPasswordChange(password: String)

    fun onTogglePasswordVisible()

    /** Open or unlock the database. */
    fun onSubmit()

    /** "Retry" on a problem with the database file. */
    fun onRetry()

    /** Pick a key file: attach one, or pick it again when it cannot be read. */
    fun onAttachKeyFile()

    /** "Sign in with biometrics" tapped - the prompt is shown only on this. */
    fun onBiometric()
}

/**
 * Choosing what is opened - another database, a new one, no key file: asked only with the matching
 * [PasswordEntryAbilities], so nothing by default; the processor of a screen that has them overrides them.
 */
interface PasswordEntryChoiceActions {

    /** Only with [PasswordEntryAbilities.changeKeyFile]. */
    fun onRemoveKeyFile() = Unit

    /** The name of the database tapped, or "pick another file" after a failure; only with [PasswordEntryAbilities.changeDatabase]. */
    fun onChangeDatabase() = Unit

    /** "Create a new database" - the way out when the remembered one is gone; only with [PasswordEntryAbilities.createDatabase]. */
    fun onCreateDatabase() = Unit
}

/**
 * What the screen lets do besides the password - the unlock screen all of it; the locked one none: the database is
 * open there, and its key file is the one it was opened with (pass-docs, Step 46).
 *
 * @param changeDatabase the name is a link that picks another file, and so is "pick another file" on a problem;
 *   without it the name is in the accent, not a link
 * @param createDatabase "create a new database" - the way out when the remembered one is gone
 * @param changeKeyFile a key file is attached and removed here; without it - only picked again when it cannot be read
 */
internal data class PasswordEntryAbilities(
    val changeDatabase: Boolean,
    val createDatabase: Boolean,
    val changeKeyFile: Boolean,
) {
    companion object {
        val Unlock = PasswordEntryAbilities(changeDatabase = true, createDatabase = true, changeKeyFile = true)
        val Locked = PasswordEntryAbilities(changeDatabase = false, createDatabase = false, changeKeyFile = false)
    }
}
