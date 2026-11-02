package ru.kino.dev.settings.pres.recyclebin

/**
 * What the setting of the recycle bin is asked to do - done by [RecycleBinProcessor]
 * (pass-docs, `docs/app/mvi-arc.md`).
 */
interface RecycleBinActions {

    /** The screen is shown: the switch and what the bin holds are read. */
    fun onLoad()

    fun onBack()

    /** "Use the recycle bin" flipped: on - kept at once; off with something in the bin - asked first. */
    fun onSetEnabled(enabled: Boolean)

    /** "Switch off" in the dialog: the bin goes with all it holds, the file is written. */
    fun onOffConfirm()

    fun onOffDismiss()

    /** "Deleted" tapped: the screen of the bin, in `vault`. */
    fun onOpenTrash()

    /** The snackbar took the message. */
    fun onMessageShown()
}
