package ru.kino.dev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.Res
import ru.kino.dev.core.pres.entry_change_database_hint
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.DISABLED_ALPHA

/**
 * The name of a database file: a link in the accent when a tap picks another file ([onChange]), with the hint under
 * it; otherwise in the accent, not a link. While [enabled] is false - the database opens, a merge runs - the link and
 * its hint grey out rather than go: what is under them does not jump (pass-docs, Step 72, fix 4; Step 108).
 */
@Composable
fun DatabaseNameLink(
    name: String,
    onChange: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = DISABLED_ALPHA)
    val changeable = onChange != null
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            style = style,
            color = if (changeable && !enabled) disabled else colors.primary,
            textDecoration = if (changeable) TextDecoration.Underline else null,
            textAlign = TextAlign.Center,
            modifier = if (onChange != null) Modifier.clickable(enabled = enabled, onClick = onChange) else Modifier,
        )
        if (changeable) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.entry_change_database_hint),
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) colors.onSurfaceVariant else disabled,
                textAlign = TextAlign.Center,
            )
        }
    }
}
