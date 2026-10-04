package ru.kino.dev.welcome.pres.common

/**
 * The part of the state the unlock and the locked screens share: what is typed and attached, the wait and
 * what went wrong. Each screen keeps it in its own state and changes it in its own processor.
 *
 * Lives in memory only - the processor state is not saved anywhere.
 *
 * @param keyFileUri the attached key file; its name alone is shown
 * @param biometricOn the "sign in with biometrics" button is shown - on for this database and the phone
 * @param biometricNotice what to say about biometrics under the button
 */
data class PasswordEntry(
    val password: String = "",
    val passwordVisible: Boolean = false,
    val keyFileUri: String? = null,
    val keyFileName: String? = null,
    val busy: Boolean = false,
    val problem: EntryProblem? = null,
    val biometricOn: Boolean = false,
    val biometricNotice: BiometricNotice? = null,
) {

    /**
     * The password as the core takes it. A database closed with a key file alone has no password, which is
     * not the same as an empty one: an empty field with a key file attached is "no password".
     */
    val passwordForCall: String?
        get() = if (password.isEmpty() && keyFileUri != null) null else password

    fun typed(password: String): PasswordEntry =
        copy(password = password, problem = problem.takeUnless { it == EntryProblem.InvalidCredentials })

    fun withKeyFile(uri: String?, name: String?): PasswordEntry =
        copy(keyFileUri = uri, keyFileName = name, problem = problem.takeUnless { it.isAboutKeyFile })
}
