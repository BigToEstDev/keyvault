package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** The clipboard of the desktop: no mark for a secret there - the clearing of Step 89 (pass-docs) stands for it. */
private class DesktopSensitiveClipboard : SensitiveClipboard {

    override fun copy(label: String, value: String) {
        val selection = StringSelection(value)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
}

@Composable
actual fun rememberSensitiveClipboard(): SensitiveClipboard = remember { DesktopSensitiveClipboard() }
