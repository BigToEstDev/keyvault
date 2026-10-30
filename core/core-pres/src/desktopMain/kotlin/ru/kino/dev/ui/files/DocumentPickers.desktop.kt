package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.awt.Window
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/*
 * The pickers of the operating system as it draws them now, in its theme - FileKit (pass-docs, Step 92): the dialog
 * of the Explorer on Windows, NSOpenPanel on macOS, the portal of the desktop on Linux. The AWT FileDialog had the
 * old one of Windows. Over the window of the app and modal to it, like the pickers of android. The uri handed back is
 * `file:`, read by the desktop `DatabaseFiles`.
 */

/** The window of the app the pickers open over - given by the desktop app; without it they open on their own. */
val LocalPickerParent = staticCompositionLocalOf<Window?> { null }

@Composable
actual fun rememberOpenDocument(kind: DocumentKind, onPicked: (uri: String) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val settings = rememberDialogSettings(kind.openTitle)
    val scope = rememberCoroutineScope()
    return remember(kind, settings, scope) {
        {
            scope.launch {
                FileKit.openFilePicker(type = kind.openFilter, dialogSettings = settings)?.let { picked(it.uri()) }
            }
        }
    }
}

@Composable
actual fun rememberCreateDocument(
    kind: DocumentKind,
    onPicked: (uri: String) -> Unit,
): (suggestedName: String) -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val settings = rememberDialogSettings(kind.saveTitle)
    val scope = rememberCoroutineScope()
    return remember(kind, settings, scope) {
        { name ->
            scope.launch {
                FileKit.openFileSaver(
                    suggestedName = name,
                    defaultExtension = kind.extension,
                    dialogSettings = settings,
                )?.let { picked(it.uri()) }
            }
        }
    }
}

@Composable
private fun rememberDialogSettings(title: StringResource): FileKitDialogSettings {
    val text = stringResource(title)
    val parent = LocalPickerParent.current
    return remember(text, parent) { FileKitDialogSettings(title = text, parentWindow = parent) }
}

/** The extension of the kind - a database is a `.kdbx`; a key file is any file. */
private val DocumentKind.extension: String?
    get() = when (this) {
        DocumentKind.Database -> "kdbx"
        DocumentKind.KeyFile -> null
    }

private val DocumentKind.openFilter: FileKitType
    get() = extension?.let { FileKitType.File(it) } ?: FileKitType.File()

private val DocumentKind.openTitle: StringResource
    get() = when (this) {
        DocumentKind.Database -> Res.string.picker_open_database
        DocumentKind.KeyFile -> Res.string.picker_open_key_file
    }

private val DocumentKind.saveTitle: StringResource
    get() = when (this) {
        DocumentKind.Database -> Res.string.picker_save_database
        DocumentKind.KeyFile -> Res.string.picker_save_key_file
    }

private fun PlatformFile.uri(): String = file.toURI().toString()
