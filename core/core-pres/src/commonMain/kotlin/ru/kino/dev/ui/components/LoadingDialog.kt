package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Work done sooner shows no dialog at all - it would only flash. */
private const val SHOW_DELAY_MS = 500L

/**
 * Something is being done that has no button of its own to show progress on - a file copied, say (pass-docs,
 * Step 111): progress and [text], no buttons. Shown only once the work takes longer than [SHOW_DELAY_MS], so a quick
 * one does not flash it. Not closed by back or a tap past it - it goes when the work is done.
 */
@Composable
fun LoadingDialog(text: String) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SHOW_DELAY_MS)
        shown = true
    }
    if (!shown) return
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(text)
            }
        },
    )
}
