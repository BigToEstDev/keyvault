package ru.kino.dev.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.action_pick_another_key_file
import ru.kino.dev.core.pres.entry_key_file_add
import ru.kino.dev.core.pres.entry_key_file_name
import ru.kino.dev.core.pres.entry_key_file_remove
import ru.kino.dev.core.pres.entry_key_file_unnamed
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.DISABLED_ALPHA

/**
 * The key file next to a password: "Has a key file" while none is attached, then its name with "Unlink" under it.
 * The two fade into each other; a [problem] with the file opens out under its name, with the way to pick another
 * (pass-docs, Step 72, fix 4; Step 108).
 *
 * @param keyFileName as the provider tells it; null - an attached file with no name to show
 * @param attachable "Has a key file" is offered - not on the locked screen, which opens with what was used
 * @param onAttach picks a key file - the first one, or another after a [problem]
 * @param onRemove unlinks it; null - it is not unlinked here
 * @param enabled false while the database opens or a merge runs: the name and the buttons grey out
 * @param alignment centred on the sign-in screen, at the start in a dialog
 */
@Composable
fun KeyFileChoice(
    attached: Boolean,
    keyFileName: String?,
    onAttach: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    attachable: Boolean = true,
    enabled: Boolean = true,
    problem: String? = null,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    AnimatedContent(
        targetState = attached,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "key file",
    ) { shown ->
        if (!shown) {
            if (attachable) {
                TextButton(onClick = onAttach, enabled = enabled) {
                    Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.entry_key_file_add))
                }
            }
        } else {
            AttachedKeyFile(keyFileName, onAttach, onRemove, enabled, problem, alignment)
        }
    }
}

@Composable
private fun AttachedKeyFile(
    keyFileName: String?,
    onAttach: () -> Unit,
    onRemove: (() -> Unit)?,
    enabled: Boolean,
    problem: String?,
    alignment: Alignment.Horizontal,
) {
    // Greys out with the buttons under it while the database opens (pass-docs, Step 72, fix 3)
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    }
    val textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.Key, contentDescription = null, tint = color)
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(
                    Res.string.entry_key_file_name,
                    keyFileName ?: stringResource(Res.string.entry_key_file_unnamed),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
        Reveal(problem) { text ->
            Column(horizontalAlignment = alignment) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = textAlign,
                )
                TextButton(onClick = onAttach, enabled = enabled) {
                    Text(stringResource(Res.string.action_pick_another_key_file))
                }
            }
        }
        if (onRemove != null) {
            TextButton(onClick = onRemove, enabled = enabled) {
                Text(stringResource(Res.string.entry_key_file_remove))
            }
        }
    }
}
