package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.FieldRowsSection
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.MenuItem
import ru.kino.dev.ui.components.MoreMenuButton
import ru.kino.dev.ui.components.SkeletonField
import ru.kino.dev.ui.components.SkeletonFieldRow
import ru.kino.dev.ui.components.SkeletonText
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.DISABLED_ALPHA
import ru.kino.dev.ui.theme.DISABLED_CONTAINER_ALPHA

/*
 * What reading and editing an entry share: the screens are one layout - the same top bar, the same cards,
 * the same fields - and differ in whether the fields take input (pass-docs, vault-our.md).
 */

/**
 * The [BackTopBar] of the entry screens. With [menu] - the three dots at the end and its items under them
 * (pass-docs, Step 50, item 2); not [menuEnabled] - while a change is under way.
 */
@Composable
internal fun EntryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    skeleton: Brush? = null,
    menu: List<MenuItem> = emptyList(),
    menuEnabled: Boolean = true,
) {
    BackTopBar(title = title, onBack = onBack, modifier = modifier, skeleton = skeleton) {
        if (menu.isNotEmpty()) {
            MoreMenuButton(
                contentDescription = stringResource(Res.string.entry_menu),
                items = menu,
                enabled = menuEnabled,
            )
        }
    }
}

/**
 * The icon of the entry on a tonal tile - the primary container, no outline (pass-docs, Step 73, item 2); one icon
 * for every entry until choosing an icon comes, a step of its own (vault-our.md, "Фича выбор иконки").
 *
 * Not [enabled] - while the entry is saved - it fades as a disabled element of Material 3 does: the fill and the
 * icon in the text color of the theme at 12% and 38%.
 */
@Composable
internal fun EntryIconFrame(modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(ICON_SIZE)
            .background(
                if (enabled) colors.primaryContainer else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
                ICON_SHAPE,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Key,
            contentDescription = null,
            tint = if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = DISABLED_ALPHA),
        )
    }
}

/**
 * The first card of the form: the icon and the title, side by side, and [below] them - the group and the preset
 * (pass-docs, Step 73, item 4).
 */
@Composable
internal fun EntryTitleSection(
    enabled: Boolean = true,
    title: @Composable (Modifier) -> Unit,
    below: @Composable ColumnScope.() -> Unit = {},
) {
    FieldSection {
        // By the bottom, not the center: an outlined field keeps room above its box for the label, so its box
        // sits lower than its middle. The tile is as tall as the box, and their bottoms meet
        Row(verticalAlignment = Alignment.Bottom) {
            EntryIconFrame(enabled = enabled)
            title(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
        below()
    }
}

/**
 * The cards of the form while the entry is read: the shape of the title card and of the fields of the standard
 * preset.
 */
@Composable
internal fun EntrySkeleton(brush: Brush) {
    FieldSection {
        Row(verticalAlignment = Alignment.Bottom) {
            SkeletonIcon(brush)
            SkeletonField(
                brush = brush,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
    FieldSection {
        repeat(SKELETON_FIELDS) { SkeletonField(brush = brush) }
    }
}

/**
 * The cards of a reading while the entry is read - the entry, one in the bin, an old version: the icon and a bar
 * of the large title beside it, rows under them, not fields (pass-docs, Step 73, item 6).
 */
@Composable
internal fun EntryReadSkeleton(brush: Brush) {
    FieldSection {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonIcon(brush)
            SkeletonText(
                brush = brush,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
    FieldRowsSection(rows = List(SKELETON_FIELDS) { { SkeletonFieldRow(brush = brush) } })
}

@Composable
private fun SkeletonIcon(brush: Brush) {
    Box(
        modifier = Modifier
            .size(ICON_SIZE)
            .clip(ICON_SHAPE)
            .background(brush),
    )
}

/** The tile of the icon: as tall as the box of an outlined field, rounded as the section it stands in. */
private val ICON_SIZE = 56.dp
private val ICON_SHAPE = RoundedCornerShape(16.dp)

/** The fields of the standard preset after the title. */
private const val SKELETON_FIELDS = 5
