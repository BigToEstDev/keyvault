package ru.kino.dev.settings.pres.entries

import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.phone.PhoneCountry

/**
 * @param entries the settings of the entries; null until read - the screen is a skeleton meanwhile
 * @param keptItems the number of versions a limit switched on takes: the one it had on this screen, or the default
 * @param keptSize the same of the size, in bytes
 * @param choosing the wheel of a limit, while open
 * @param trimming "trim the history?", while asked
 * @param saving the part written into the file - every row waits, that one shows progress
 * @param phone the prefix of a new phone field; null until read (pass-docs, Step 77)
 * @param message for the snackbar, until it takes it
 */
data class EntriesState(
    val entries: DatabaseEntries? = null,
    val keptItems: Int = HistoryChoices.DEFAULT_ITEMS,
    val keptSize: Long = HistoryChoices.DEFAULT_SIZE,
    val choosing: HistoryLimit? = null,
    val trimming: TrimDialog? = null,
    val saving: EntriesPart? = null,
    val phone: PhonePrefix? = null,
    val message: EntriesMessage? = null,
)

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
