package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.vault.pres.common.ClosableDialog

/**
 * The filters of the list: the title and "close" only, for now. What to show and how to sort are
 * pass-docs, Step 41.
 */
@Composable
internal fun FilterDialog(onClose: () -> Unit) {
    ClosableDialog(title = stringResource(R.string.vault_filters), onClose = onClose)
}
