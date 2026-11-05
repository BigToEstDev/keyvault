package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.FieldRowsSection
import ru.kino.dev.ui.components.FileRow
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.files.fileSizeText
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.pres.entryedit.EntryEditActions
import ru.kino.dev.vault.pres.entryedit.FormAttachment

/**
 * The attachments on the form - the last card, after the own fields (pass-docs, Step 103): each with its cross, then
 * "+ Add attachment", waiting while a file picked is read. At the limit - no button, as for own fields; one removed
 * brings it back (item 5).
 */
@Composable
internal fun EntryEditActions.FormAttachments(
    attachments: List<FormAttachment>,
    canAdd: Boolean,
    adding: Boolean,
    enabled: Boolean,
) {
    val files: List<@Composable () -> Unit> = attachments.mapIndexed { index, attachment ->
        {
            FileRow(
                name = attachment.name,
                size = fileSizeText(attachment.size),
                enabled = enabled,
                actions = {
                    IconButton(onClick = { onRemoveAttachment(index) }, enabled = enabled) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_remove))
                    }
                },
            )
        }
    }
    val add: @Composable () -> Unit = {
        Box(modifier = Modifier.padding(BUTTON_PADDING)) {
            SecondaryButton(
                text = stringResource(Res.string.entry_add_attachment),
                onClick = { onAddAttachment() },
                enabled = enabled,
                loading = adding,
            )
        }
    }
    FieldRowsSection(rows = if (canAdd) files + add else files)
}

private val BUTTON_PADDING = 16.dp
