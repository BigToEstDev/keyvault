package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

// The browser of the system; a desktop with none - nothing happens, the link stays there to be copied
@Composable
actual fun rememberLinkOpener(): (link: String) -> Unit = remember {
    { link ->
        val desktop = Desktop.getDesktop().takeIf { Desktop.isDesktopSupported() }
        if (desktop?.isSupported(Desktop.Action.BROWSE) == true) runCatching { desktop.browse(URI(link)) }
    }
}
