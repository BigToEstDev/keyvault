package ru.kino.dev.settings.pres.file.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.SkeletonText

/**
 * The rows of the file while it is read: the name, the file, the format - a title and a line under it each - and
 * the compression (pass-docs, Step 63).
 */
@Composable
internal fun FileSkeleton(brush: Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(ROWS) { RowSkeleton(brush) }
        RowSkeleton(brush, lines = COMPRESSION_LINES)
    }
}

@Composable
private fun RowSkeleton(brush: Brush, lines: Int = 1) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge, widthFraction = TITLE_WIDTH)
            repeat(lines) {
                SkeletonText(brush = brush, style = MaterialTheme.typography.bodyMedium, widthFraction = TEXT_WIDTH)
            }
        }
    }
}

/** The name, the file, the format. */
private const val ROWS = 3

/** The two choices of the compression. */
private const val COMPRESSION_LINES = 2

private const val TITLE_WIDTH = 0.4f
private const val TEXT_WIDTH = 0.7f
