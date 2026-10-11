package ru.kino.dev.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * An icon with a number on a badge over its corner - a folder and the entries inside it, in the database
 * and in every group.
 *
 * The badge is the accent of the theme (`tertiary`). Numbers above [MAX_SHOWN] are cut to "999+": the badge
 * is for a glance, not for counting.
 */
@Composable
fun CountBadgeIcon(
    count: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Folder,
    contentDescription: String? = null,
) {
    BadgedBox(
        modifier = modifier,
        badge = {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Text(if (count > MAX_SHOWN) "$MAX_SHOWN+" else count.toString())
            }
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private const val MAX_SHOWN = 999
