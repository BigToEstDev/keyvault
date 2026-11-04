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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.entries.components.EntriesSkeleton
import ru.kino.dev.settings.pres.entries.components.PhonePrefixRow
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.WheelPickerDialog
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.phone.PhoneCountryDialog
import ru.kino.dev.ui.phone.phoneCountryName
import ru.kino.dev.ui.phone.phonePrefix
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.ui.res.stringResource

/**
 * The entries of the database: the fields hidden on every entry, how much history an entry keeps (pass-docs,
 * Step 65), the recycle bin (Step 99, item 11), the prefix of a new phone field.
 *
 * @param canGoBack false - a page of the container of a wide window: its tab closes it, not "back" (Step 101)
 */
@Composable
internal fun EntriesScreen(
    dbKey: String,
    canGoBack: Boolean = true,
    processor: EntriesProcessor = koinProcessor(dbKey),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.onLoad() }
    // Back from the bin, it holds another number
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.onLoadBin() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntriesMessage.NotSaved -> Res.string.message_file_not_saved
                EntriesMessage.Failed -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_entries),
                    onBack = processor::onBack,
                    back = canGoBack,
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
                    protectionRows(entries.protection, state.saving, processor)
                    historyRows(entries, state.saving, processor)
                    state.bin?.let { binRows(it, state.saving, processor) }
                    state.phone?.let { phoneRows(it, processor) }
                }
            }
        }
    }

    processor.Dialogs(state)
}

private fun LazyListScope.protectionRows(
    protection: FieldProtection,
    saving: EntriesPart?,
    actions: EntriesActions,
) {
    item(key = "hidden") {
        SettingsSectionHeader(
            title = stringResource(Res.string.settings_entries_hidden_group),
            hint = stringResource(Res.string.settings_entries_hidden_hint),
        )
    }
    item(key = "fields") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                PROTECTED_KEYS.forEach { key ->
                    SwitchRow(
                        title = fieldLabel(key),
                        checked = protection.protects(key),
                        onCheckedChange = { actions.onSetProtection(protection.with(key, it)) },
                        // Only the card written waits; a tap elsewhere meanwhile is not taken (Step 99, item 12)
                        enabled = saving != EntriesPart.Protection,
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
    actions: EntriesActions,
) {
    val history = entries.history
    item(key = "history") {
        // Set apart from the group above, as the sections of the main screen
        SettingsSectionHeader(title = stringResource(Res.string.settings_history_group), top = 8.dp)
    }
    item(key = "items") {
        LimitCard(
            limitTitle = stringResource(Res.string.settings_history_limit_items),
            valueTitle = stringResource(Res.string.settings_history_items),
            value = history.maxItems?.toString(),
            saving = saving,
            part = EntriesPart.Items,
            onLimit = { actions.onLimitItems(it) },
            onChoose = { actions.onChoose(HistoryLimit.Items) },
        )
    }
    item(key = "size") {
        LimitCard(
            limitTitle = stringResource(Res.string.settings_history_limit_size),
            valueTitle = stringResource(Res.string.settings_history_size),
            value = history.maxSize?.let { sizeLabel(it) },
            saving = saving,
            part = EntriesPart.Size,
            onLimit = { actions.onLimitSize(it) },
            onChoose = { actions.onChoose(HistoryLimit.Size) },
        )
    }
}

/**
 * The recycle bin (pass-docs, Step 70; here since Step 99, item 11): whether it is used - progress while the file is
 * written - and how many entries it holds; that row opens the bin, empty - it waits.
 */
private fun LazyListScope.binRows(bin: RecycleBin, saving: EntriesPart?, actions: EntriesActions) {
    item(key = "bin") {
        SettingsSectionHeader(title = stringResource(Res.string.settings_recycle_bin), top = 8.dp)
    }
    item(key = "bin-use") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = stringResource(Res.string.settings_recycle_bin_use),
                    text = stringResource(
                        if (bin.enabled) Res.string.settings_recycle_bin_use_on else Res.string.settings_recycle_bin_use_off,
                    ),
                    checked = bin.enabled,
                    onCheckedChange = { actions.onSetBinEnabled(it) },
                    enabled = saving != EntriesPart.RecycleBin,
                )
                // The key derivation takes seconds
                if (saving == EntriesPart.RecycleBin) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
    }
    item(key = "bin-deleted") {
        Card(modifier = Modifier.fillMaxWidth()) {
            ValueRow(
                title = stringResource(Res.string.settings_recycle_bin_deleted),
                value = if (bin.count == 0) stringResource(Res.string.settings_recycle_bin_empty) else bin.count.toString(),
                onClick = { actions.onOpenTrash() },
                enabled = bin.count > 0 && saving != EntriesPart.RecycleBin,
            )
        }
    }
}

/**
 * The prefix of a new phone field (pass-docs, Step 77, decision 6): the switch, and under it - only while the prefix
 * is remembered - the country, as the limits of the history show their values.
 */
private fun LazyListScope.phoneRows(phone: PhonePrefix, actions: EntriesActions) {
    item(key = "phone") {
        SettingsSectionHeader(title = stringResource(Res.string.settings_phone_group), top = 8.dp)
    }
    item(key = "prefix") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = stringResource(Res.string.settings_phone_remember),
                    checked = phone.remember,
                    onCheckedChange = { actions.onRememberPrefix(it) },
                )
                AnimatedVisibility(
                    visible = phone.remember,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    PhonePrefixRow(
                        title = stringResource(Res.string.settings_phone_prefix),
                        value = "${phonePrefix(phone.region)} ${phoneCountryName(phone.region)}",
                        onClick = { actions.onChoosePrefix() },
                    )
                }
            }
        }
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
                enabled = saving != part,
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
                    enabled = saving != part,
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
private fun EntriesActions.Dialogs(state: EntriesState) {
    val history = state.entries?.history
    when (state.choosing) {
        HistoryLimit.Items -> history?.maxItems?.let { items ->
            WheelPickerDialog(
                title = stringResource(Res.string.settings_history_items),
                options = HistoryChoices.items(items),
                selected = items,
                label = { it.toString() },
                onConfirm = { onSelectItems(it) },
                onDismiss = { onDismissChoice() },
            )
        }

        HistoryLimit.Size -> history?.maxSize?.let { size ->
            WheelPickerDialog(
                title = stringResource(Res.string.settings_history_size),
                options = HistoryChoices.sizes(size),
                selected = size,
                label = { sizeLabel(it) },
                onConfirm = { onSelectSize(it) },
                onDismiss = { onDismissChoice() },
            )
        }

        null -> Unit
    }

    state.phone?.countries?.let { countries ->
        PhoneCountryDialog(
            countries = countries,
            selected = state.phone.region,
            onChoose = { onPrefixChosen(it) },
            onClose = { onDismissPrefix() },
        )
    }

    if (state.trimming != null) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_history_trim_title),
            text = stringResource(Res.string.settings_history_trim_text),
            confirmText = stringResource(Res.string.settings_history_trim_confirm),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = { onTrimConfirm() },
            onDismiss = { onTrimDismiss() },
            destructive = true,
        )
    }

    if (state.confirmingBinOff) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_recycle_bin_off_title),
            text = stringResource(Res.string.settings_recycle_bin_off_text),
            confirmText = stringResource(Res.string.settings_recycle_bin_off_confirm),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = { onBinOffConfirm() },
            onDismiss = { onBinOffDismiss() },
            destructive = true,
        )
    }
}

/** "10 MB"; a size of another program that is no whole megabyte - "3.5 MB", in the way of the language. */
@Composable
private fun sizeLabel(bytes: Long): String {
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    return stringResource(Res.string.settings_megabytes_short, number.format(bytes.toDouble() / MEGABYTE))
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
