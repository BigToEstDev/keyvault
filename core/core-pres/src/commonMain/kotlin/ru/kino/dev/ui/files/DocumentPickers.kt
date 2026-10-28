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

/** Picks an existing file. The returned function launches the picker. */
@Composable
expect fun rememberOpenDocument(onPicked: (uri: String) -> Unit): () -> Unit

/** "Save as": the user picks the folder and the name, the returned function takes the name to suggest. */
@Composable
expect fun rememberCreateDocument(onPicked: (uri: String) -> Unit): (suggestedName: String) -> Unit
