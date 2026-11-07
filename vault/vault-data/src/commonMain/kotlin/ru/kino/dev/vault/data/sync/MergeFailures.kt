package ru.kino.dev.vault.data.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.DatabaseFileException
import ru.kino.dev.vault.sync.MergeFailure

/**
 * Runs a merge so that it fails with a [MergeFailure] and nothing else (pass-docs, Step 108): the kinds of the core
 * by the table below, a file that cannot be read - [MergeFailure.FileUnreachable]. Cancellation passes untouched.
 */
internal suspend fun <T> mergeCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: MergeFailure) {
        throw e
    } catch (e: CoreException) {
        throw e.toMergeFailure()
    } catch (e: DatabaseFileException) {
        throw MergeFailure.FileUnreachable(e)
    } catch (e: Exception) {
        throw MergeFailure.Unexpected(e)
    }

internal fun CoreException.toMergeFailure(): MergeFailure = when (kind) {
    "MergeFailedCredentialsChanged" -> MergeFailure.CredentialsChanged(this)
    "InvalidCredentials" -> MergeFailure.WrongCredentials(this)
    "NotSameDatabase" -> MergeFailure.NotSameDatabase(this)
    "SameDatabase" -> MergeFailure.SameDatabase(this)
    "DbLocked" -> MergeFailure.Locked(this)
    CredentialLimits.KIND_KEY_FILE_TOO_LARGE -> MergeFailure.KeyFileTooLarge(this)
    in UNSUPPORTED -> MergeFailure.UnsupportedFile(this)
    in CORRUPTED -> MergeFailure.CorruptedFile(this)
    // DbKeyNotFound, Decryption, UnexpectedError, the bridge's own, PasswordTooLong - the field does not let a
    // longer one through
    else -> MergeFailure.Unexpected(this)
}

// The same kinds as on opening a database (welcome-data, WelcomeFailures): the second file is read the same way
private val UNSUPPORTED = setOf(
    "InvalidKeePassFile",
    "OldUnsupportedKeePass1",
    "OldUnsupportedKdbxFormat",
    "UnsupportedCipher",
    "UnsupportedKdfAlgorithm",
    "SupportedOnlyArgon2dKdfAlgorithm",
)

// Io is not expected on a read from bytes; if it comes anyway, the bytes are what failed (Step 30)
private val CORRUPTED = setOf("HeaderCorrupted", "ContentCorrupted", "XmlParsingFailed", "Io")
