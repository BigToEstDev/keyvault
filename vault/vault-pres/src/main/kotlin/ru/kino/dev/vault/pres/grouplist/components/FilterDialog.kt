package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.RadioGroup
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.VaultFilter
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.pres.common.ClosableDialog

/**
 * The filters of the list (pass-docs, `vault-our.md`, "Диалог фильтров"): what to show and in what order the
 * entries come. The cross drops what is chosen; "Apply" puts it in effect, "Reset" - the default.
 *
 * @param draft what is chosen in the dialog so far
 * @param saving the choice is being kept: the buttons wait, the choices take nothing
 */
@Composable
internal fun FilterDialog(
    draft: VaultFilter,
    saving: Boolean,
    onVisibility: (Visibility) -> Unit,
    onOrder: (CreationOrder) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    ClosableDialog(title = stringResource(R.string.vault_filters), onClose = onClose) {
        Column(
            // The dialog leaves less room on the right, for its cross
            modifier = Modifier.padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RadioGroup(
                title = stringResource(R.string.vault_filter_visibility),
                options = Visibility.entries,
                selected = draft.visibility,
                label = { stringResource(it.text) },
                onSelect = onVisibility,
                enabled = !saving,
            )
            RadioGroup(
                title = stringResource(R.string.vault_filter_order),
                options = CreationOrder.entries,
                selected = draft.order,
                label = { stringResource(it.text) },
                onSelect = onOrder,
                enabled = !saving,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(text = stringResource(R.string.action_apply), onClick = onApply, loading = saving)
                SecondaryButton(text = stringResource(R.string.action_reset), onClick = onReset, enabled = !saving)
            }
        }
    }
}

private val Visibility.text: Int
    get() = when (this) {
        Visibility.All -> R.string.vault_filter_all
        Visibility.GroupsOnly -> R.string.vault_filter_groups_only
        Visibility.EntriesOnly -> R.string.vault_filter_entries_only
    }

private val CreationOrder.text: Int
    get() = when (this) {
        CreationOrder.NewestFirst -> R.string.vault_filter_newest_first
        CreationOrder.OldestFirst -> R.string.vault_filter_oldest_first
    }
