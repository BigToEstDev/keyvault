package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.WelcomeCardFrame

/**
 * Entering a password in a wide window (pass-docs, Step 94): the column of a phone, all of it on a card in the
 * middle of the window ([WelcomeCardFrame]). Until the state is read the shield alone on the card; once read it
 * shrinks and the rest opens out under it, as on a phone.
 */
@Composable
internal fun PasswordEntryActions.PasswordEntryTablet(
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

    WelcomeCardFrame {
        ShieldImage(modifier = Modifier.size(shieldSize))
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

/** The shield over the password on the card. */
private val SHIELD_SIZE = 96.dp
