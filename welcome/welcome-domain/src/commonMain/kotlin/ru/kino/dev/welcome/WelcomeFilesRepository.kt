package ru.kino.dev.welcome

/**
 * The files the user picks in `welcome`, before any of them is a database.
 *
 * Every failure is a [WelcomeFailure].
 */
interface WelcomeFilesRepository {

    /**
     * The name to show for a picked file, or null when the provider does not tell - the database picked on
     * the welcome screen, a key file picked on the unlock screen, before either is opened.
     */
    suspend fun displayName(uri: String): String?

    /**
     * Whether the picked database is still there - asked when the unlock screen shows it, so that a file
     * deleted outside the app is said before the password is typed, not after.
     *
     * A hint: an offline provider may answer from its cache, and opening reports for real.
     */
    suspend fun isReachable(uri: String): Boolean

    /**
     * Whether writing into [uri] would destroy something - "save as" for the database or for the key file
     * handed back an existing file rather than a new one.
     *
     * The screen asks this before writing and warns: an existing key file may be the key of another
     * database, and writing over it locks that one for good.
     *
     * @param file which file this is, for the failure when it cannot be reached
     */
    suspend fun wouldOverwrite(uri: String, file: PickedFile): Boolean

    /**
     * Checks an existing file picked as the key file of a new database - before "save as" for the database,
     * so that a video picked by mistake is said right away, not after an empty database file was made.
     * Asks the provider for the size, does not read the file.
     *
     * @throws WelcomeFailure.KeyFileTooLarge over [ru.kino.dev.core.CredentialLimits.KEY_FILE_MAX_SIZE]
     * @throws WelcomeFailure.FileUnreachable when the file is not there
     */
    suspend fun checkKeyFile(uri: String)

    /**
     * Generates a new key file and writes it into [uri], the file "save as" handed back - the key file
     * backup screen, before the database is created.
     *
     * The content never leaves this call: it goes from the core straight into the file, and creating the
     * database reads it back from there.
     */
    suspend fun writeNewKeyFile(uri: String)
}
