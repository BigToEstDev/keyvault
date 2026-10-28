package ru.kino.dev.vault.credentials

/**
 * What the key of the open database is made of, and its password checked again - what the settings ask before
 * they keep the password for biometrics (pass-docs, Step 59, item 7). Of `vault`: the database is open.
 *
 * Failures of the core come as `CoreException`, as with the lock - `DbKeyNotFound` for a database closed
 * meanwhile.
 */
interface DatabaseCredentials {

    /** Whether the key has a password - one with a key file alone gets no biometrics, there is nothing to keep. */
    suspend fun hasPassword(dbKey: String): Boolean

    /**
     * Whether [password] is the password of the database - checked alone, the key file not asked for, at once;
     * locked or not, nothing changes. False for a database without a password.
     */
    suspend fun verifyPassword(dbKey: String, password: String): Boolean
}
