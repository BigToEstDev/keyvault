package ru.kino.dev.settings.pres.security

import ru.kino.dev.core.Cipher
import ru.kino.dev.core.KdfVariant
import kotlin.time.Duration

sealed interface SecurityAction {

    /** The screen is shown: the security is read, then one KDF pass is measured. */
    data object Load : SecurityAction

    data object Back : SecurityAction

    /** A cipher chosen: kept and the file written at once. */
    data class SelectCipher(val cipher: Cipher) : SecurityAction

    /** An Argon2 variant chosen: kept and the file written at once. */
    data class SelectKdf(val kdf: KdfVariant) : SecurityAction

    /** The rounds tapped: the wheel with them. */
    data object ChooseRounds : SecurityAction

    /** "Save" on the wheel of the rounds: kept and the file written. */
    data class SelectRounds(val rounds: Long) : SecurityAction

    data object DismissRounds : SecurityAction

    /** The snackbar took the message. */
    data object MessageShown : SecurityAction
}

/** What part of the screen is written into the file - every row waits, that one shows progress. */
enum class SecurityPart { Cipher, Kdf, Rounds }

/** One KDF pass on this phone: the open time is a pass times the rounds. */
sealed interface KdfPass {

    /** Measured in the background, after the screen is read - a skeleton meanwhile. */
    data object Measuring : KdfPass

    data class Measured(val pass: Duration) : KdfPass

    /** It could not be measured: no time is shown. */
    data object Unknown : KdfPass
}

/** What the snackbar says. */
enum class SecurityMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,
}
