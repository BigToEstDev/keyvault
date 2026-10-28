package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/*
 * The pickers of the operating system: the AWT FileDialog is the native dialog on Windows and macOS. Modal, like the
 * pickers of android - the window waits for it. The uri handed back is `file:`, read by the desktop `DatabaseFiles`.
 */

@Composable
actual fun rememberOpenDocument(onPicked: (uri: String) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    return remember { { pick(FileDialog.LOAD, suggestedName = null)?.let(picked) } }
}

@Composable
actual fun rememberCreateDocument(onPicked: (uri: String) -> Unit): (suggestedName: String) -> Unit {
    val picked by rememberUpdatedState(onPicked)
    return remember { { name -> pick(FileDialog.SAVE, suggestedName = name)?.let(picked) } }
}

private fun pick(mode: Int, suggestedName: String?): String? {
    val dialog = FileDialog(null as Frame?).apply {
        this.mode = mode
        file = suggestedName
        isVisible = true
    }
    val name = dialog.file ?: return null
    return File(dialog.directory, name).toURI().toString()
}
