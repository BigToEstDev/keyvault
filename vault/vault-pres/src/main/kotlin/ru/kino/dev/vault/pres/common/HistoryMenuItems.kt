package ru.kino.dev.vault.pres.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.MenuItem

/** "History" - the old versions of an entry (pass-docs, Step 51, item 3). */
@Composable
internal fun historyMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.entry_history), Icons.Filled.History, onClick)

/** "Restore this version" - of a history (pass-docs, Step 51, item 4). */
@Composable
internal fun restoreMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_restore), Icons.Filled.Restore, onClick)

/** "Delete history" - every old version of an entry (pass-docs, Step 51, item 5). */
@Composable
internal fun deleteHistoryMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_delete_all), Icons.Filled.Delete, onClick)

/** "Delete version" - of a history. */
@Composable
internal fun deleteVersionMenuItem(onClick: () -> Unit) =
    MenuItem(stringResource(R.string.history_delete_version), Icons.Filled.Delete, onClick)
