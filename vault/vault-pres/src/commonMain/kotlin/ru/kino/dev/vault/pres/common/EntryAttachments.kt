package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.FieldRowsSection
import ru.kino.dev.ui.components.FileRow
import ru.kino.dev.ui.files.fileSizeText
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.vault.attachment.EntryAttachment

/**
 * The attachments of an entry to read - the last card, under its fields (pass-docs, Step 103): the name and the size
 * of each, "Download" by it when [downloadable]; none - no card.
 *
 * @param downloading the attachment being written into its file - its button waits, the others take nothing
 */
@Composable
internal fun AttachmentDownloadActions.ReadAttachments(
    attachments: List<EntryAttachment>,
    downloadable: Boolean,
    downloading: EntryAttachment?,
) {
    if (attachments.isEmpty()) return
    FieldRowsSection(
        rows = attachments.mapIndexed { index, attachment ->
            {
                FileRow(
                    name = attachment.name,
                    size = fileSizeText(attachment.size),
                    actions = if (downloadable) {
                        {
                            DownloadButton(
                                waiting = attachment == downloading,
                                enabled = downloading == null,
                                onClick = { onDownload(index) },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        },
    )
}

/** "Download" - the arrow down; while the file is written, a progress in its place. */
@Composable
private fun DownloadButton(waiting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled && !waiting) {
        Box(contentAlignment = Alignment.Center) {
            if (waiting) {
                CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Download, contentDescription = stringResource(Res.string.entry_attachment_download))
            }
        }
    }
}

private val PROGRESS_SIZE = 20.dp
