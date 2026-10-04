package ru.kino.dev.vault.pres.entrieslist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.SecondaryButton

/**
 * Locks the database for real (`lock_kdbx`) - the only way to the locked screen until `vault` locks on its
 * own. What it is for on a device is in pass-docs, Step 32. Goes when automatic locking comes.
 */
@Composable
internal fun DebugLockButton(locking: Boolean, onLock: () -> Unit) {
    SecondaryButton(text = stringResource(R.string.entries_debug_lock), onClick = onLock, enabled = !locking)
}
