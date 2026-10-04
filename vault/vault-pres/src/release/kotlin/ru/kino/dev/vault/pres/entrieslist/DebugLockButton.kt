package ru.kino.dev.vault.pres.entrieslist

import androidx.compose.runtime.Composable

/** Nothing in a release build: the debug lock button is in the debug source set only. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun DebugLockButton(locking: Boolean, onLock: () -> Unit) = Unit
