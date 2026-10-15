package ru.kino.dev.vault.pres.entryread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.CopyButton
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.SensitiveClipboard
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.ui.components.rememberSensitiveClipboard
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.StandardPreset
import ru.kino.dev.vault.pres.common.EntrySection
import ru.kino.dev.vault.pres.common.EntrySkeleton
import ru.kino.dev.vault.pres.common.EntryTitleSection
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.fieldLabel

@Composable
internal fun EntryReadScreen(
    dbKey: String,
    entryId: String,
    processor: EntryReadProcessor = hiltViewModel<EntryReadProcessor, EntryReadProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, entryId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Every time the screen is shown: back from the form, the entry may differ
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(EntryReadAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryReadMessage.LoadFailed -> R.string.entry_message_load_failed
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntryReadAction.MessageShown) }

    // While the entry is read, one brush for the title and the fields - their skeletons run together
    val skeleton = if (state.content == null && state.loading) skeletonBrush() else null

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                // The title of the entry; a skeleton while it is read, nothing if it could not be
                val title = state.content?.value(StandardPreset.TITLE)
                EntryTopBar(
                    title = title?.ifEmpty { stringResource(R.string.entry_untitled) }.orEmpty(),
                    onBack = { processor.action(EntryReadAction.Back) },
                    skeleton = skeleton,
                )
            },
            floatingActionButton = {
                if (state.content != null) {
                    FloatingActionButton(onClick = { processor.action(EntryReadAction.Edit) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.entry_edit))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val content = state.content
                when {
                    content != null -> EntryFields(state = state, content = content, action = processor::action)
                    skeleton != null -> EntrySkeleton(brush = skeleton)
                }
            }
        }
    }
}

/** The fields of the entry that hold something; an empty title reads "untitled" and is not copied. */
@Composable
private fun EntryFields(state: EntryReadState, content: EntryContent, action: (EntryReadAction) -> Unit) {
    val clipboard = rememberSensitiveClipboard()

    EntryTitleSection { modifier ->
        val title = content.value(StandardPreset.TITLE)
        ReadField(
            label = fieldLabel(StandardPreset.TITLE),
            value = title.ifEmpty { stringResource(R.string.entry_untitled) },
            clipboard = clipboard.takeIf { title.isNotEmpty() },
            modifier = modifier,
        )
    }

    val shownPreset = READ_ORDER.filter { content.value(it).isNotEmpty() }
    val shownOwn = content.ownFields.withIndex().filter { it.value.value.isNotEmpty() }
    if (shownPreset.isNotEmpty()) {
        EntrySection { PresetFields(state, content, shownPreset, clipboard, action) }
    }

    // A card of its own, as on the form
    if (shownOwn.isNotEmpty()) {
        EntrySection { OwnFields(state, shownOwn, clipboard, action) }
    }
}

@Composable
private fun PresetFields(
    state: EntryReadState,
    content: EntryContent,
    keys: List<String>,
    clipboard: SensitiveClipboard,
    action: (EntryReadAction) -> Unit,
) {
    keys.forEach { key ->
        if (key == StandardPreset.PASSWORD) {
            val label = fieldLabel(key)
            val value = content.value(key)
            PasswordField(
                value = value,
                onValueChange = {},
                label = label,
                visible = state.passwordVisible,
                onToggleVisible = { action(EntryReadAction.TogglePassword) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                actions = { CopyButton(onCopy = { clipboard.copy(label, value) }) },
            )
        } else {
            ReadField(
                label = fieldLabel(key),
                value = content.value(key),
                clipboard = clipboard,
                multiLine = key == StandardPreset.NOTES,
            )
        }
    }
}

@Composable
private fun OwnFields(
    state: EntryReadState,
    fields: List<IndexedValue<EntryField>>,
    clipboard: SensitiveClipboard,
    action: (EntryReadAction) -> Unit,
) {
    fields.forEach { (index, field) ->
        val shown = index in state.shownOwn
        ReadField(
            label = field.key,
            value = field.value,
            clipboard = clipboard,
            hidden = field.protected && !shown,
            onToggleShown = if (field.protected) {
                { action(EntryReadAction.ToggleOwn(index)) }
            } else {
                null
            },
            shown = shown,
        )
    }
}

/**
 * A field that only shows its value, with "copy" - when [clipboard] is given - and the eye of a protected one.
 */
@Composable
private fun ReadField(
    label: String,
    value: String,
    clipboard: SensitiveClipboard?,
    modifier: Modifier = Modifier,
    multiLine: Boolean = false,
    hidden: Boolean = false,
    shown: Boolean = false,
    onToggleShown: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = {},
        label = label,
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        hidden = hidden,
        singleLine = !multiLine,
        actions = if (clipboard == null && onToggleShown == null) {
            null
        } else {
            {
                // The eye first, then "copy" - as on the password
                onToggleShown?.let { VisibilityToggle(visible = shown, onToggle = it) }
                clipboard?.let { CopyButton(onCopy = { it.copy(label, value) }) }
            }
        },
    )
}

/** The fields of the standard preset in the second card, in the order of the form. */
private val READ_ORDER = StandardPreset.preset.fields.map { it.key } - StandardPreset.TITLE

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
