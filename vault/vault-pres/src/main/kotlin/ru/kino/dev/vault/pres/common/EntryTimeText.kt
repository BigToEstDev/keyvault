package ru.kino.dev.vault.pres.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A time of an entry - seconds since the epoch, UTC - as the screens of an entry write it: `17.10.2026 14:05`,
 * in the zone of the device, the same in every locale (pass-docs, Step 51, item 2).
 */
internal fun entryTimeText(epochSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    ENTRY_TIME.withZone(zone).format(Instant.ofEpochSecond(epochSeconds))

private val ENTRY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
