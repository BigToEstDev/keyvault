package ru.kino.dev.welcome.pres.common

import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure

/**
 * What went wrong, as a screen of `welcome` answers it - one case per row of the table in pass-docs,
 * `decisions.md`, "Ошибки". The screen branches on this, never on a message.
 */
sealed interface EntryProblem {

    /** Under the password field; the password stays. */
    data object InvalidCredentials : EntryProblem

    /** The password is hidden - no password will help; only another file. */
    data object UnsupportedFile : EntryProblem

    /** The password is hidden; retry - a sync may finish in a minute - or another file. */
    data object CorruptedFile : EntryProblem

    /**
     * Retry, or another file - the one that failed: for the database the password is hidden, for the key file
     * the answer is under the key file and the password stays.
     */
    data class Unreachable(val file: PickedFile) : EntryProblem

    /** Under the key file, with the limit; the password stays. */
    data class KeyFileTooLarge(val maxMebibytes: Long) : EntryProblem

    /** Ours: "something went wrong", the cause goes to the log. */
    data object Unexpected : EntryProblem
}

internal fun WelcomeFailure.toProblem(): EntryProblem = when (this) {
    is WelcomeFailure.InvalidCredentials -> EntryProblem.InvalidCredentials
    is WelcomeFailure.UnsupportedFile -> EntryProblem.UnsupportedFile
    is WelcomeFailure.CorruptedFile -> EntryProblem.CorruptedFile
    is WelcomeFailure.FileUnreachable -> EntryProblem.Unreachable(file)
    is WelcomeFailure.KeyFileTooLarge -> EntryProblem.KeyFileTooLarge(maxBytes / BYTES_IN_MEBIBYTE)
    is WelcomeFailure.Unexpected -> EntryProblem.Unexpected
}

/** Whether the problem is with the database file itself, so that typing a password makes no sense. */
internal val EntryProblem?.hidesPassword: Boolean
    get() = this == EntryProblem.UnsupportedFile ||
        this == EntryProblem.CorruptedFile ||
        this == EntryProblem.Unreachable(PickedFile.Database)

/** Whether the problem is with the key file - shown under it. */
internal val EntryProblem?.isAboutKeyFile: Boolean
    get() = this is EntryProblem.KeyFileTooLarge || this == EntryProblem.Unreachable(PickedFile.KeyFile)

private const val BYTES_IN_MEBIBYTE = 1024L * 1024L
