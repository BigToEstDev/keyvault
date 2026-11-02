package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.welcome.pres.common.PasswordEntry

/**
 * Entering a password in one column, as on a phone: the shield, the name, the password under it. Until the state is
 * read the shield alone, large, in the middle; once read it shrinks and the rest opens out under it, so the shield
 * slides to its place (pass-docs, Step 72, fix 4).
 */
@Composable
internal fun PasswordEntryActions.PasswordEntryMobile(
    databaseName: String,
    entry: PasswordEntry,
    submitText: String,
    abilities: PasswordEntryAbilities,
    read: Boolean,
    subtitle: String?,
) {
    val shieldSize by animateDpAsState(
        targetValue = if (read) SHIELD_SIZE else SHIELD_SIZE_LARGE,
        animationSpec = tween(READ_MILLIS),
        label = "shield",
    )

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ShieldImage(modifier = Modifier.size(shieldSize))
            // The column is centred: as this opens out, the shield moves up to its place
            AnimatedVisibility(
                visible = read,
                enter = expandVertically(tween(READ_MILLIS)) + fadeIn(tween(READ_MILLIS)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    DatabaseHeader(
                        databaseName = databaseName,
                        subtitle = subtitle,
                        abilities = abilities,
                        enabled = !entry.busy,
                    )
                    Spacer(Modifier.height(32.dp))
                    EntryForm(entry = entry, submitText = submitText, abilities = abilities)
                }
            }
        }
    }
}

/** The shield over the password: smaller than on the welcome screen, which has nothing else to show. */
private val SHIELD_SIZE = 96.dp
