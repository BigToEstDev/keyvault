package ru.kino.dev.settings.pres.entries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.NumberFormat
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.settings.pres.entries.components.EntriesSkeleton
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.WheelPickerDialog
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.preset.fieldLabel

/**
 * The entries of the database: the fields hidden on every entry, how much history an entry keeps (pass-docs,
 * Step 65).
 */
@Composable
internal fun EntriesScreen(
    dbKey: String,
    processor: EntriesProcessor = hiltViewModel<EntriesProcessor, EntriesProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.action(EntriesAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntriesMessage.NotSaved -> R.string.message_file_not_saved
                EntriesMessage.Failed -> R.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntriesAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_entries),
                    onBack = { processor.action(EntriesAction.Back) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val entries = state.entries
                if (entries == null) {
                    // Until read there is nothing to show for real: the shapes of the screen
                    item(key = "skeleton") { EntriesSkeleton(skeletonBrush()) }
                } else {
                    protectionRows(entries.protection, state.saving, processor::action)
                    historyRows(entries, state.saving, processor::action)
                }
            }
        }
    }

    Dialogs(state, processor::action)
}

private fun LazyListScope.protectionRows(
    protection: FieldProtection,
    saving: EntriesPart?,
    action: (EntriesAction) -> Unit,
) {
    item(key = "hidden") {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_entries_hidden_group),
            hint = stringResource(R.string.settings_entries_hidden_hint),
        )
    }
    item(key = "fields") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                PROTECTED_KEYS.forEach { key ->
                    SwitchRow(
                        title = fieldLabel(key),
                        checked = protection.protects(key),
                        onCheckedChange = { action(EntriesAction.SetProtection(protection.with(key, it))) },
                        enabled = saving == null,
                    )
                }
                SavingProgress(saving == EntriesPart.Protection)
            }
        }
    }
}

private fun LazyListScope.historyRows(
    entries: DatabaseEntries,
    saving: EntriesPart?,
    action: (EntriesAction) -> Unit,
) {
    val history = entries.history
    item(key = "history") {
        // Set apart from the group above, as the sections of the main screen
        SettingsSectionHeader(title = stringResource(R.string.settings_history_group), top = 8.dp)
    }
    item(key = "items") {
        LimitCard(
            limitTitle = stringResource(R.string.settings_history_limit_items),
            valueTitle = stringResource(R.string.settings_history_items),
            value = history.maxItems?.toString(),
            saving = saving,
            part = EntriesPart.Items,
            onLimit = { action(EntriesAction.LimitItems(it)) },
            onChoose = { action(EntriesAction.Choose(HistoryLimit.Items)) },
        )
    }
    item(key = "size") {
        LimitCard(
            limitTitle = stringResource(R.string.settings_history_limit_size),
            valueTitle = stringResource(R.string.settings_history_size),
            value = history.maxSize?.let { sizeLabel(it) },
            saving = saving,
            part = EntriesPart.Size,
            onLimit = { action(EntriesAction.LimitSize(it)) },
            onChoose = { action(EntriesAction.Choose(HistoryLimit.Size)) },
        )
    }
}

/**
 * A limit of the history: the switch, and the value under it - only while there is a limit: the card shrinks and
 * grows with it.
 *
 * @param value the limit as shown; null - no limit
 */
@Composable
private fun LimitCard(
    limitTitle: String,
    valueTitle: String,
    value: String?,
    saving: EntriesPart?,
    part: EntriesPart,
    onLimit: (Boolean) -> Unit,
    onChoose: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            SwitchRow(
                title = limitTitle,
                checked = value != null,
                onCheckedChange = onLimit,
                enabled = saving == null,
            )
            // The last limit stays in the row while it shrinks away, not an empty value
            var shown by remember { mutableStateOf(value) }
            if (value != null) shown = value
            AnimatedVisibility(
                visible = value != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ValueRow(
                    title = valueTitle,
                    value = shown.orEmpty(),
                    onClick = onChoose,
                    enabled = saving == null,
                )
            }
            SavingProgress(saving == part)
        }
    }
}

/** Under the rows written into the file, while it is written - the key derivation takes seconds. */
@Composable
private fun SavingProgress(visible: Boolean) {
    if (visible) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
private fun Dialogs(state: EntriesState, action: (EntriesAction) -> Unit) {
    val history = state.entries?.history
    when (state.choosing) {
        HistoryLimit.Items -> history?.maxItems?.let { items ->
            WheelPickerDialog(
                title = stringResource(R.string.settings_history_items),
                options = HistoryChoices.items(items),
                selected = items,
                label = { it.toString() },
                onConfirm = { action(EntriesAction.SelectItems(it)) },
                onDismiss = { action(EntriesAction.DismissChoice) },
            )
        }

        HistoryLimit.Size -> history?.maxSize?.let { size ->
            WheelPickerDialog(
                title = stringResource(R.string.settings_history_size),
                options = HistoryChoices.sizes(size),
                selected = size,
                label = { sizeLabel(it) },
                onConfirm = { action(EntriesAction.SelectSize(it)) },
                onDismiss = { action(EntriesAction.DismissChoice) },
            )
        }

        null -> Unit
    }

    if (state.trimming != null) {
        ConfirmDialog(
            title = stringResource(R.string.settings_history_trim_title),
            text = stringResource(R.string.settings_history_trim_text),
            confirmText = stringResource(R.string.settings_history_trim_confirm),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = { action(EntriesAction.TrimConfirm) },
            onDismiss = { action(EntriesAction.TrimDismiss) },
            destructive = true,
        )
    }
}

/** "10 MB"; a size of another program that is no whole megabyte - "3.5 MB", in the way of the language. */
@Composable
private fun sizeLabel(bytes: Long): String {
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    return stringResource(R.string.settings_megabytes_short, number.format(bytes.toDouble() / MEGABYTE))
}

/** The same protection with the field of [key] private or not. */
private fun FieldProtection.with(key: String, private: Boolean): FieldProtection = when (key) {
    StandardPreset.TITLE -> copy(title = private)
    StandardPreset.USER_NAME -> copy(userName = private)
    StandardPreset.PASSWORD -> copy(password = private)
    StandardPreset.URL -> copy(url = private)
    else -> copy(notes = private)
}

/** The standard fields KeePass protects, in the order of the form. */
private val PROTECTED_KEYS = listOf(
    StandardPreset.TITLE,
    StandardPreset.USER_NAME,
    StandardPreset.PASSWORD,
    StandardPreset.URL,
    StandardPreset.NOTES,
)
