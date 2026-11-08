package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable

/**
 * The share menu of the system for a file another app may read - its uri from `SharedFiles` (pass-docs, Step 111).
 * The returned function opens it; what the person picks there is the system's - nothing comes back.
 *
 * Android only: on the desktop the function does nothing - nothing asks for it there, `SharedFiles` is not available.
 */
@Composable
expect fun rememberShareFile(): (uri: String) -> Unit
