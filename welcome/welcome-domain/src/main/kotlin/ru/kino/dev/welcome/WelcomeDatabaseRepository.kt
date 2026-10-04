package ru.kino.dev.welcome

/**
 * Getting to an open database: opening a file, unlocking a locked one, creating a new one.
 *
 * Each method is one step of a scenario of `welcome` (pass-docs, `docs/android/screens/welcome/`), and each
 * one that succeeds has finished everything before it returns: the database is open in the core, access
 * to its files is kept for the next launch, and it is the remembered [CurrentDatabase]. What comes back is
 * the `dbKey` alone - the one thing that crosses into `vault`.
 *
 * Every failure is a [WelcomeFailure]. A failure leaves the remembered database as it was: it is not
 * forgotten for a provider that is offline, and not replaced by a file that did not open.
 */
interface WelcomeDatabaseRepository {

    /**
     * Opens the database in [uri] - the unlock screen, on start and after another file was picked.
     *
     * Reads the whole file and runs the kdf - seconds, not milliseconds.
     *
     * @param password null for a database closed with a key file alone, which is not the same as an empty
     * password
     * @param keyFileUri the key file to open with, or null for none; read anew, never kept in memory
     * @return the `dbKey` to hand to `vault`
     */
    suspend fun open(uri: String, password: String?, keyFileUri: String?): String

    /**
     * Unlocks the database the core still holds, locked while open - the locked screen.
     *
     * Quick: the credentials are checked against the key the core kept, the database file is not read.
     *
     * A key file other than the remembered one - picked on the locked screen because the remembered one
     * could not be reached - is remembered once it unlocked the database, with access to it kept, so the
     * next launch opens with it. When that cannot be fixed the database is locked again.
     *
     * @param password null for a database closed with a key file alone
     * @param keyFileUri the key file to unlock with, usually the remembered one; read anew, never kept in
     * memory
     */
    suspend fun unlock(dbKey: String, password: String?, keyFileUri: String?)

    /**
     * Creates a database in [uri] - the empty file "save as" has just made - and leaves it open.
     *
     * The last step of creating one, right after the file was picked. The name inside the database is the
     * name of the file without `.kdbx`. When this fails the empty file is deleted, as far as the provider
     * allows, so an abandoned attempt leaves nothing behind.
     *
     * @param password at most [ru.kino.dev.core.CredentialLimits.PASSWORD_MAX_CHARS] characters
     * @param keyFileUri the key file written by [WelcomeFilesRepository.writeNewKeyFile], or null for none -
     * read back from where it was saved, so the database is closed with what is actually on disk
     * @return the `dbKey` to hand to `vault`
     */
    suspend fun create(uri: String, password: String, keyFileUri: String?): String
}
