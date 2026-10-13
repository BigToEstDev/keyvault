package ru.kino.dev.vault.pres.grouplist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.skeletonBrush

/**
 * A card while the tree is being read: the shape of a [GroupCard] - the icon, the name - painted with
 * [skeletonBrush].
 */
@Composable
internal fun SkeletonCard(modifier: Modifier = Modifier) {
    val brush = skeletonBrush()

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brush),
            )
            Spacer(Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(NAME_WIDTH)
                    .clip(MaterialTheme.shapes.small)
                    .background(brush),
            )
        }
    }
}

/** Share of the row the name bar takes. */
private const val NAME_WIDTH = 0.6f
