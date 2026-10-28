package ru.kino.dev.vault.group

/**
 * How many entries a group holds - two ways to count them, the settings choose which one the screen shows
 * (pass-docs, Step 58).
 *
 * @param direct the entries right in the group
 * @param nested those and the entries of every visible group below it
 */
data class EntryCount(val direct: Int, val nested: Int) {

    companion object {
        /** A group that is not there, or not read yet. */
        val None = EntryCount(direct = 0, nested = 0)
    }
}
