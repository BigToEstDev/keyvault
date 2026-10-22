package ru.kino.dev.vault.security

import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.Encryption
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.settings.signin.RememberedKeyFile
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard
import kotlin.coroutines.cancellation.CancellationException

/**
 * Changing the security of the open database from the settings: one change, one write of the file (pass-docs,
 * Step 67). What else a change makes stale goes with it here, not on the screen: a new password forgets the
 * biometrics of the database, a new key file - or none - is what the sign-in screens remember.
 *
 * Both happen once the change is made, written or [SecurityFailure.NotSaved]: in memory the database already
 * has the new credentials - an unlock goes with them, and the next save writes them.
 *
 * Each change runs inside [guard], so a lock waits for its write to end.
 *
 * Throws [SecurityFailure] and nothing else.
 */
class SecurityEditor(
    private val security: SecurityRepository,
    private val database: VaultDatabaseRepository,
    private val biometrics: BiometricRepository,
    private val rememberedKeyFile: RememberedKeyFile,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /** Encrypts with [encryption] and saves. The same encryption again is not a change: nothing is written. */
    suspend fun setEncryption(dbKey: String, encryption: Encryption): Unit = guard.exclusive {
        if (encryption == security.read(dbKey).encryption) return@exclusive
        security.changeEncryption(dbKey, encryption)
        save(dbKey)
    }

    /**
     * Gives the database the password [new] and saves. A database with a password needs it as [current] first;
     * one with only a key file gets its first password without it.
     */
    suspend fun setPassword(dbKey: String, current: String?, new: String): Unit = guard.exclusive {
        if (security.read(dbKey).credentials.password) {
            if (current == null || !security.verifyPassword(dbKey, current)) throw SecurityFailure.WrongPassword()
        }
        security.changeCredentials(dbKey, CredentialsChange.SetPassword(new))
        try {
            save(dbKey)
        } finally {
            forgetBiometrics(dbKey)
        }
    }

    /** Gives the database the key file at [uri] - a first one, or another - and saves. */
    suspend fun setKeyFile(dbKey: String, uri: String): Unit = guard.exclusive {
        val keyFile = security.readKeyFile(uri)
        val name = keyFile.name
        try {
            security.changeCredentials(dbKey, CredentialsChange.SetKeyFile(keyFile))
        } finally {
            keyFile.wipe()
        }
        saveAndRemember(dbKey, keyFileUri = uri, keyFileName = name)
    }

    /** Takes the key file away and saves - only from a database with a password. */
    suspend fun removeKeyFile(dbKey: String): Unit = guard.exclusive {
        if (!security.read(dbKey).credentials.password) throw SecurityFailure.LastFactor()
        security.changeCredentials(dbKey, CredentialsChange.RemoveKeyFile)
        saveAndRemember(dbKey, keyFileUri = null, keyFileName = null)
    }

    // NotSaved still has the key file changed in memory, so it is remembered as well - and NotSaved is what the
    // screen says then, not the remembering
    private suspend fun saveAndRemember(dbKey: String, keyFileUri: String?, keyFileName: String?) {
        val notSaved = try {
            save(dbKey)
            null
        } catch (e: SecurityFailure.NotSaved) {
            e
        }
        try {
            rememberedKeyFile.remember(dbKey, keyFileUri, keyFileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw notSaved ?: SecurityFailure.KeyFileNotRemembered(e)
        }
        notSaved?.let { throw it }
    }

    // A forget that failed leaves a password that no longer opens the database: the sign-in forgets it then, on
    // InvalidCredentials (pass-docs, welcome/decisions.md) - with one error more on the screen, but nothing broken.
    // Not a failure of the change, which is made
    private suspend fun forgetBiometrics(dbKey: String) {
        try {
            biometrics.forget(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw SecurityFailure.NotSaved(e)
        }
    }
}
