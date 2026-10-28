package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.PrimaryButton
import ru.kino.dev.ui.components.RadioGroup
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.res.stringResource
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
    ClosableDialog(title = stringResource(Res.string.vault_filters), onClose = onClose) {
        Column(
            // The dialog leaves less room on the right, for its cross
            modifier = Modifier.padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RadioGroup(
                title = stringResource(Res.string.vault_filter_visibility),
                options = Visibility.entries,
                selected = draft.visibility,
                label = { stringResource(it.text) },
                onSelect = onVisibility,
                enabled = !saving,
            )
            RadioGroup(
                title = stringResource(Res.string.vault_filter_order),
                options = CreationOrder.entries,
                selected = draft.order,
                label = { stringResource(it.text) },
                onSelect = onOrder,
                enabled = !saving,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(text = stringResource(Res.string.action_apply), onClick = onApply, loading = saving)
                SecondaryButton(text = stringResource(Res.string.action_reset), onClick = onReset, enabled = !saving)
            }
        }
    }
}

private val Visibility.text: StringResource
    get() = when (this) {
        Visibility.All -> Res.string.vault_filter_all
        Visibility.GroupsOnly -> Res.string.vault_filter_groups_only
        Visibility.EntriesOnly -> Res.string.vault_filter_entries_only
    }

private val CreationOrder.text: StringResource
    get() = when (this) {
        CreationOrder.NewestFirst -> Res.string.vault_filter_newest_first
        CreationOrder.OldestFirst -> Res.string.vault_filter_oldest_first
    }
