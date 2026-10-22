package ru.kino.dev.settings.signin

/**
 * The key file the app remembers for signing in to a database, changed from the settings (pass-docs, Step 67).
 *
 * The sign-in screens read the remembered key file anew on every opening; once the settings give the database
 * another key file, or none, the old one opens it no more. The remembered database is `welcome`'s, so `vault` - and
 * the settings on top of it - reach it through this, never seeing anything else of `welcome`.
 */
interface RememberedKeyFile {

    /**
     * Remembers [keyFileUri] with its [keyFileName] for [dbUri], or no key file when both are null. A database that
     * is not the remembered one is left alone. Throws an `IOException` when it could not be written.
     */
    suspend fun remember(dbUri: String, keyFileUri: String?, keyFileName: String?)
}
