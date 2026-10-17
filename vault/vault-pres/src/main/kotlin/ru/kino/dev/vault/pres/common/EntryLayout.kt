package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.SkeletonField
import ru.kino.dev.ui.components.SkeletonText

/*
 * What reading and editing an entry share: the screens are one layout - the same top bar, the same cards,
 * the same fields - and differ in whether the fields take input (pass-docs, vault-our.md).
 */

/**
 * "Back" and the [title] of the screen - one line, cut short with an ellipsis. With [skeleton] the title is
 * not known yet - a bar painted with it stands in, so the title does not flash in (pass-docs, Step 40, fix 13).
 * With [menu] - the three dots at the end and its items under them (pass-docs, Step 50, item 2); not
 * [menuEnabled] - while a change is under way.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
        if (skeleton != null) {
            SkeletonText(
                brush = skeleton,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        }
        if (menu.isNotEmpty()) {
            MoreMenuButton(
                contentDescription = stringResource(R.string.entry_menu),
                items = menu,
                enabled = menuEnabled,
            )
        }
    }
}

/**
 * A section of the entry: its fields one under another, in a card outlined in the accent of the palette over a
 * light fill - lighter than the backdrop in either theme. The default fill of a card is not of our palette and
 * looks muddy over the gradient (pass-docs, Step 40, fix 3).
 */
@Composable
internal fun EntrySection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Light over a dark backdrop is its text color, faint; over a light one - the surface, nearly whole
    val fill = if (colors.background.luminance() < HALF) {
        colors.onSurface.copy(alpha = DARK_FILL_ALPHA)
    } else {
        colors.surface.copy(alpha = LIGHT_FILL_ALPHA)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fill),
        border = BorderStroke(1.dp, colors.tertiary),
    ) {
        Column(
            modifier = Modifier.padding(SECTION_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * The icon of the entry in a frame - one icon for every entry until choosing an icon comes, a step of its
 * own (vault-our.md, "Фича выбор иконки").
 *
 * Not [enabled] - while the entry is saved - it fades as a disabled field of Material 3 does: the text color
 * of the theme at 38%.
 */
@Composable
internal fun EntryIconFrame(modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    Box(
        modifier = modifier
            .size(56.dp)
            .border(1.dp, if (enabled) colors.outline else disabled, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Key, contentDescription = null, tint = if (enabled) colors.primary else disabled)
    }
}

/** The first card of every preset: the icon and the title, side by side. */
@Composable
internal fun EntryTitleSection(enabled: Boolean = true, title: @Composable (Modifier) -> Unit) {
    EntrySection {
        // By the bottom, not the center: an outlined field keeps room above its box for the label, so its box
        // sits lower than its middle. The frame is as tall as the box, and their bottoms meet
        Row(verticalAlignment = Alignment.Bottom) {
            EntryIconFrame(enabled = enabled)
            title(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
}

/**
 * The card of the preset, between the group and the fields: a button outlined in the primary color with the
 * [name] of the preset. It opens the choice of presets. Not [enabled], its outline fades as that of a disabled
 * outlined button of Material 3: the text color of the theme at 12%; the text fades by itself.
 */
@Composable
internal fun EntryPresetSection(name: String, onClick: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    EntrySection {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            border = BorderStroke(
                1.dp,
                if (enabled) colors.primary else colors.onSurface.copy(alpha = DISABLED_OUTLINE_ALPHA),
            ),
        ) {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The name of [preset]: one's own is named by the person, the standard one - from the resources. */
@Composable
internal fun presetName(preset: EntryPreset): String = preset.name ?: stringResource(R.string.entry_preset_standard)

/** The label of a field of the standard preset; an own field is labelled with its key. */
@Composable
internal fun fieldLabel(key: String): String = when (key) {
    StandardPreset.TITLE -> stringResource(R.string.entry_field_title)
    StandardPreset.USER_NAME -> stringResource(R.string.entry_field_user_name)
    StandardPreset.PASSWORD -> stringResource(R.string.entry_field_password)
    StandardPreset.SERVICE -> stringResource(R.string.entry_field_service)
    StandardPreset.URL -> stringResource(R.string.entry_field_url)
    StandardPreset.NOTES -> stringResource(R.string.entry_field_notes)
    else -> key
}

/** The cards while the entry is read: the shape of the title card and of the fields of the standard preset. */
@Composable
internal fun EntrySkeleton(brush: Brush) {
    EntrySection {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(brush),
            )
            SkeletonField(
                brush = brush,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
    }
    EntrySection {
        repeat(SKELETON_FIELDS) { SkeletonField(brush = brush) }
    }
}

/** Room inside a section; what stands under the sections in line with their content takes it too. */
internal val SECTION_PADDING = 16.dp

/** What Material 3 gives disabled content and a disabled outline: the text color at these. */
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val DISABLED_OUTLINE_ALPHA = 0.12f

/** The fill of a section over the dark theme and over the light one. */
private const val DARK_FILL_ALPHA = 0.08f
private const val LIGHT_FILL_ALPHA = 0.85f
private const val HALF = 0.5f

/** The fields of the standard preset after the title. */
private const val SKELETON_FIELDS = 5
