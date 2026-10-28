package ru.kino.dev.settings.pres.security.components

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
 * The rows of the security while they are read: the credentials, the two choices of the encryption, then the
 * rounds with memory and lanes (pass-docs, Step 67).
 */
@Composable
internal fun SecuritySkeleton(brush: Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSkeleton(brush, rows = CREDENTIAL_ROWS)
        repeat(CHOICES) { CardSkeleton(brush, rows = CHOICE_ROWS) }
        CardSkeleton(brush, rows = KDF_ROWS)
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

/** The password and the key file. */
private const val CREDENTIAL_ROWS = 2

/** The cipher and the key derivation. */
private const val CHOICES = 2

/** The title and two options. */
private const val CHOICE_ROWS = 3

/** The rounds, the memory, the lanes. */
private const val KDF_ROWS = 3

private const val TITLE_WIDTH = 0.5f
