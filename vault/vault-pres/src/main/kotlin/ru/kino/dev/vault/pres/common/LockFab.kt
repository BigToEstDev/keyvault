package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.LocalAppDisplay

/**
 * Locks the database now, not waiting for the timer - at the bottom on the left, on the screens of the open
 * database, when `Lock button` is on in the settings (pass-docs, Step 59); nothing otherwise. In the accent
 * color (`tertiary`), apart from the fab of the screen - as the debug button was (owner, 21.10.26).
 *
 * @param locking the lock is under way: progress, and no more taps
 */
@Composable
internal fun LockFab(locking: Boolean, onLock: () -> Unit, modifier: Modifier = Modifier) {
    if (!LocalAppDisplay.current.lockButton) return
    FloatingActionButton(
        onClick = { if (!locking) onLock() },
        modifier = modifier
            .navigationBarsPadding()
            .padding(LOCK_FAB_MARGIN),
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
            Icon(Icons.Filled.Lock, contentDescription = stringResource(Res.string.vault_lock))
        }
    }
}

/** The margin of the fabs of Material 3 - the same as the fab of the screen on the other side. */
private val LOCK_FAB_MARGIN = 16.dp
