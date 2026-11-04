package ru.kino.dev.settings.pres.presets.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.SkeletonText

/**
 * The switch "Use presets" while it is read: its title, the line under it and the switch, in the shapes of the row -
 * not a disabled switch that turns on a moment later (pass-docs, Step 62).
 */
@Composable
internal fun UsePresetsSkeleton(brush: Brush) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge, widthFraction = TITLE_WIDTH)
                SkeletonText(brush = brush, style = MaterialTheme.typography.bodyMedium, widthFraction = TEXT_WIDTH)
            }
            Box(
                modifier = Modifier
                    .size(width = SWITCH_WIDTH, height = SWITCH_HEIGHT)
                    .clip(CircleShape)
                    .background(brush),
            )
        }
    }
}

/** A preset while the list is read: the line of its name, as tall as its card ([ru.kino.dev.ui.components.MenuCard]). */
@Composable
internal fun PresetCardSkeleton(brush: Brush) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge, widthFraction = NAME_WIDTH)
        }
    }
}

private const val TITLE_WIDTH = 0.6f
private const val TEXT_WIDTH = 0.85f
private const val NAME_WIDTH = 0.4f

/** The track of a switch of Material 3. */
private val SWITCH_WIDTH = 52.dp
private val SWITCH_HEIGHT = 32.dp
