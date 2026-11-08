package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable

// No share menu on the desktop (pass-docs, Step 111): "Send" is not shown there, nothing calls this
@Composable
actual fun rememberShareFile(): (uri: String) -> Unit = {}
