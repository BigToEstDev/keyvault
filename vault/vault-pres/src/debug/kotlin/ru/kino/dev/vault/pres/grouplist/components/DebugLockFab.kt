package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R

/**
 * Locks the database for real (`lock_kdbx`) - the only way to the locked screen until `vault` locks on its
 * own. In the accent color, to stand apart from the real buttons. Goes when automatic locking comes.
 */
@Composable
internal fun DebugLockFab(locking: Boolean, onLock: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = { if (!locking) onLock() },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
    ) {
        if (locking) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.vault_debug_lock))
        }
    }
}
