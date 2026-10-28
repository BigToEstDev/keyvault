package ru.kino.dev.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Hands a message of the state to the snackbar and tells the processor it is taken at once - the snackbar
 * shows it on a scope of its own, so clearing the message does not cut it short.
 *
 * @param text the message, null when there is none
 * @param onShown the processor clears the message: the same one can then come again
 */
@Composable
fun SnackbarMessage(text: String?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(text) {
        if (text != null) {
            scope.launch { snackbar.showSnackbar(text) }
            onShown()
        }
    }
}
