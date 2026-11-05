package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable

/*
 * The system pickers, shared by `welcome` and `settings` (pass-docs, Step 67). Launched by the screen when its
 * processor asks, and what they hand back goes to the processor as an action - the processor never sees the
 * platform: a `content:` uri on android, a `file:` one on the desktop, both read by `DatabaseFiles`.
 *
 * Only single documents: access to a folder is never asked for (pass-docs, `decisions.md`, "Одна текущая
 * база"). A cancelled picker hands back nothing and nothing happens.
 */

/**
 * What a picker is for - the desktop gives its picker the title and the filter of it (pass-docs, Step 92); android
 * shows every file either way: a kdbx has no mime type there.
 */
enum class DocumentKind {
    /** A database - a `.kdbx`. */
    Database,

    /** A key file - any file at all. */
    KeyFile,

    /** A file attached to an entry, or one an attachment is saved as - any file at all (pass-docs, Step 103). */
    Attachment,
}

/** Picks an existing file of [kind]. The returned function launches the picker. */
@Composable
expect fun rememberOpenDocument(kind: DocumentKind, onPicked: (uri: String) -> Unit): () -> Unit

/**
 * "Save as" of [kind]: the user picks the folder and the name, the returned function takes the name to suggest.
 */
@Composable
expect fun rememberCreateDocument(kind: DocumentKind, onPicked: (uri: String) -> Unit): (suggestedName: String) -> Unit
