package ru.kino.dev.settings.pres.recyclebin

/**
 * @param enabled whether the database uses the bin; null until read - the screen is a skeleton meanwhile
 * @param count the entries in the bin
 * @param confirmingOff "switch the bin off?", while asked - the bin holds something
 * @param saving the switch is written into the file - it waits and shows progress
 * @param message for the snackbar, until it takes it
 */
data class RecycleBinState(
    val enabled: Boolean? = null,
    val count: Int = 0,
    val confirmingOff: Boolean = false,
    val saving: Boolean = false,
    val message: RecycleBinMessage? = null,
)

/** What the snackbar says. */
enum class RecycleBinMessage {
    /** The change is made, but the file was not written: it waits for the next save. */
    NotSaved,

    /** The core refused - the database closed meanwhile, or a bug. */
    Failed,
}
