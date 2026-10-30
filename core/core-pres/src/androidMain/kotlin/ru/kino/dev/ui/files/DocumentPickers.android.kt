package ru.kino.dev.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

// The pickers of the storage access framework: the uri handed back is `content:`. The kind changes nothing here -
// every file is shown, and the framework draws its own title

/** Any file: a kdbx has no mime type providers agree on. */
private const val ANY_FILE = "*/*"

/** What "save as" creates: plain bytes, so that a provider does not append an extension of its own. */
private const val BINARY_FILE = "application/octet-stream"

/** Picks an existing file - `ACTION_OPEN_DOCUMENT`. The returned function launches the picker. */
@Composable
actual fun rememberOpenDocument(kind: DocumentKind, onPicked: (uri: String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return { launcher.launch(arrayOf(ANY_FILE)) }
}

/**
 * "Save as" - `ACTION_CREATE_DOCUMENT`: the user picks the folder and the name, the returned function takes
 * the name to suggest.
 */
@Composable
actual fun rememberCreateDocument(
    kind: DocumentKind,
    onPicked: (uri: String) -> Unit,
): (suggestedName: String) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BINARY_FILE)) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return { name -> launcher.launch(name) }
}
