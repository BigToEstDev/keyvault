package ru.kino.dev.settings.pres.entries.components

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
 * The rows of the entries while they are read: the card of the hidden fields, then the two limits of the history
 * (pass-docs, Step 65).
 */
@Composable
internal fun EntriesSkeleton(brush: Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSkeleton(brush, rows = FIELDS)
        repeat(LIMITS) { CardSkeleton(brush, rows = LIMIT_ROWS) }
    }
}

@Composable
private fun CardSkeleton(brush: Brush, rows: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            repeat(rows) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge, widthFraction = TITLE_WIDTH)
                }
            }
        }
    }
}

/** Title, user name, password, URL, notes. */
private const val FIELDS = 5

/** The number of versions and the size. */
private const val LIMITS = 2

/** The switch and the value. */
private const val LIMIT_ROWS = 2

private const val TITLE_WIDTH = 0.5f
