package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.WelcomeCard

/**
 * Entering a password in a wide window (pass-docs, Step 94): the window in halves - on the left the shield, large,
 * and the name of the database: what is opened; on the right a card with what is asked to get in. The shield is in
 * its place from the start; once the state is read the name and the card come into view by it, nothing moves.
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
    // The name keeps its room while unread - the shield over it does not move as it comes
    val headerAlpha by animateFloatAsState(
        targetValue = if (read) 1f else 0f,
        animationSpec = tween(READ_MILLIS),
        label = "header",
    )

    GradientBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(HALF_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShieldImage(modifier = Modifier.size(SHIELD_SIZE_LARGE))
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.alpha(headerAlpha)) {
                    // Not to be tapped before it is seen
                    DatabaseHeader(
                        databaseName = databaseName,
                        subtitle = subtitle,
                        abilities = abilities,
                        enabled = read && !entry.busy,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(HALF_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedVisibility(visible = read, enter = fadeIn(tween(READ_MILLIS))) {
                    WelcomeCard(
                        modifier = Modifier
                            .widthIn(max = CARD_MAX_WIDTH)
                            .fillMaxWidth(),
                    ) {
                        EntryForm(entry = entry, submitText = submitText, abilities = abilities)
                    }
                }
            }
        }
    }
}

/** Round what each half holds - off the edges of the window and off the other half. */
private val HALF_PADDING = 32.dp

/** The card holds a field and its buttons - wider than this they stretch to no use. */
private val CARD_MAX_WIDTH = 400.dp
