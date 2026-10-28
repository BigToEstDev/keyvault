package ru.kino.dev.settings.pres.recyclebin.components

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

/** The two cards of the recycle bin while it is read: the switch with its hint, then what it holds (pass-docs, Step 70). */
@Composable
internal fun RecycleBinSkeleton(brush: Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSkeleton(brush, hint = true)
        CardSkeleton(brush, hint = false)
    }
}

@Composable
private fun CardSkeleton(brush: Brush, hint: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge, widthFraction = TITLE_WIDTH)
            if (hint) SkeletonText(brush = brush, style = MaterialTheme.typography.bodyMedium, widthFraction = HINT_WIDTH)
        }
    }
}

private const val TITLE_WIDTH = 0.5f
private const val HINT_WIDTH = 0.8f
