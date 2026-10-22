package ru.kino.dev.vault.pres.common

import kotlin.time.Duration.Companion.milliseconds

/**
 * How long a search waits for the typing to stop - in the list of `vault` and in the recycle bin; a constant of the
 * owner (pass-docs, `vault/decisions.md`, "Поиск").
 */
internal val SEARCH_DELAY = 300.milliseconds
