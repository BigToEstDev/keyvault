package ru.kino.dev.settings.pres.entries

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.phone.PhoneCountry

sealed interface EntriesAction {

    /** The screen is shown: the settings of the entries are read. */
    data object Load : EntriesAction

    data object Back : EntriesAction

    /** A switch of the hidden fields flipped: kept and the file written at once. */
    data class SetProtection(val protection: FieldProtection) : EntriesAction

    /** "Limit the number of versions": off - no limit; on - the number it had, or the default. */
    data class LimitItems(val limited: Boolean) : EntriesAction

    /** "Limit the history size": off - no limit; on - the size it had, or the default. */
    data class LimitSize(val limited: Boolean) : EntriesAction

    /** A value of the history tapped: the wheel with it. */
    data class Choose(val limit: HistoryLimit) : EntriesAction

    /** The number of versions taken on the wheel. */
    data class SelectItems(val items: Int) : EntriesAction

    /** The size taken on the wheel, in bytes. */
    data class SelectSize(val bytes: Long) : EntriesAction

    data object DismissChoice : EntriesAction

    /** "Trim" in the dialog: the lower limit is kept and the file written. */
    data object TrimConfirm : EntriesAction

    data object TrimDismiss : EntriesAction

    /** "Remember the phone prefix" flipped (pass-docs, Step 77, decision 6). */
    data class RememberPrefix(val on: Boolean) : EntriesAction

    /** "The phone prefix by default" tapped: the countries to choose from. */
    data object ChoosePrefix : EntriesAction

    /** The country picked for the prefix by default - its code. */
    data class PrefixChosen(val region: String) : EntriesAction

    data object DismissPrefix : EntriesAction

    /** The snackbar took the message. */
    data object MessageShown : EntriesAction
}

/** A limit of the history, as a row of the screen. */
enum class HistoryLimit { Items, Size }

/** What part of the screen is written into the file - its rows wait, it shows progress. */
enum class EntriesPart { Protection, Items, Size }

/**
 * "Trim the history?" - asked before [history] is kept: it drops versions already kept, at once and for good.
 *
 * @param part the row that asked it
 */
data class TrimDialog(val history: HistoryLimits, val part: EntriesPart)

/**
 * The prefix a new phone field starts with - of the app, not of the database (pass-docs, Step 77).
 *
 * @param region the country it is of: the one chosen, or of the device
 * @param countries to choose from, while the choice is open
 */
data class PhonePrefix(val remember: Boolean, val region: String, val countries: List<PhoneCountry>? = null)

/** What the snackbar says. */
enum class EntriesMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,
}
