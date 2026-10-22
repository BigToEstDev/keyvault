package ru.kino.dev.settings.pres.security

/**
 * What the wheel of the rounds offers (pass-docs, `settings/settings.md`, «Безопасность»): 3 - what the app creates
 * a database with, 64 MiB and 3 passes of RFC 9106 - to 10. The rounds of a database from elsewhere outside them are
 * shown as the nearest end; the file keeps its own until a choice is saved - a setting of the database is not
 * changed on the person's behalf.
 */
object RoundsChoices {

    const val MIN = 3L
    const val MAX = 10L

    val OPTIONS: List<Long> = (MIN..MAX).toList()

    /** The rounds of the file as the screen shows them. */
    fun shown(iterations: Long): Long = iterations.coerceIn(MIN, MAX)
}
