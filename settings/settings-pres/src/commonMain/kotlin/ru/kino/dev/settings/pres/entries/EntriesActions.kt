package ru.kino.dev.settings.pres.entries

import ru.kino.dev.core.FieldProtection

/** What the settings of entries is asked to do - done by [EntriesProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface EntriesActions {

    /** The screen is shown: the settings of the entries are read. */
    fun onLoad()

    fun onBack()

    /** A switch of the hidden fields flipped: kept and the file written at once. */
    fun onSetProtection(protection: FieldProtection)

    /** "Limit the number of versions": off - no limit; on - the number it had, or the default. */
    fun onLimitItems(limited: Boolean)

    /** "Limit the history size": off - no limit; on - the size it had, or the default. */
    fun onLimitSize(limited: Boolean)

    /** A value of the history tapped: the wheel with it. */
    fun onChoose(limit: HistoryLimit)

    /** The number of versions taken on the wheel. */
    fun onSelectItems(items: Int)

    /** The size taken on the wheel, in bytes. */
    fun onSelectSize(bytes: Long)

    fun onDismissChoice()

    /** "Trim" in the dialog: the lower limit is kept and the file written. */
    fun onTrimConfirm()

    fun onTrimDismiss()

    /** "Remember the phone prefix" flipped (pass-docs, Step 77, decision 6). */
    fun onRememberPrefix(on: Boolean)

    /** "The phone prefix by default" tapped: the countries to choose from. */
    fun onChoosePrefix()

    /** The country picked for the prefix by default - its code. */
    fun onPrefixChosen(region: String)

    fun onDismissPrefix()

    /** The screen is shown - first, or back from the bin: what the bin holds is read again (Step 99, item 11). */
    fun onLoadBin()

    /** "Use the recycle bin" flipped: on - kept at once; off with something in the bin - asked first. */
    fun onSetBinEnabled(enabled: Boolean)

    /** "Switch off" in the dialog: the bin goes with all it holds, the file is written. */
    fun onBinOffConfirm()

    fun onBinOffDismiss()

    /** "Deleted" tapped: the screen of the bin, in `vault`. */
    fun onOpenTrash()

    /** The snackbar took the message. */
    fun onMessageShown()
}
