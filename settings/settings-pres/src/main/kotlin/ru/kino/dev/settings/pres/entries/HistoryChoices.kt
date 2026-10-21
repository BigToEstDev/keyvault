package ru.kino.dev.settings.pres.entries

/**
 * What the wheels of the history offer (pass-docs, Step 65). A value of the file not among them - 6 MB of a new
 * database, as KeePass makes it, or anything another program wrote - is offered too, in its place: it is never
 * lost by opening the wheel.
 */
object HistoryChoices {

    /** A limit switched on where there was none. */
    const val DEFAULT_ITEMS = 10
    const val DEFAULT_SIZE = 10L * MEGABYTE

    private val ITEMS = (0..50).toList()
    private val SIZES = listOf(2L, 5L, 10L, 15L, 20L, 50L).map { it * MEGABYTE }

    /** The numbers of versions, with [current]. */
    fun items(current: Int): List<Int> = (ITEMS + current).distinct().sorted()

    /** The sizes in bytes, with [current]. */
    fun sizes(current: Long): List<Long> = (SIZES + current).distinct().sorted()
}

/** A megabyte as KeePass counts it in its settings. */
const val MEGABYTE = 1024L * 1024
