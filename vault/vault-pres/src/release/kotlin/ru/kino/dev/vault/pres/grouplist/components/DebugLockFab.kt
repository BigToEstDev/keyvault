package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable

/** Nothing in a release build: the debug lock button is in the debug source set only. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun DebugLockFab(locking: Boolean, onLock: () -> Unit) = Unit
