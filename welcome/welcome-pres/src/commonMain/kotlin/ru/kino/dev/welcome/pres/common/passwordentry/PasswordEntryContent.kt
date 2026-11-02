package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.ShieldImage
import ru.kino.dev.welcome.pres.common.PasswordEntry
import ru.kino.dev.welcome.pres.common.hidesPassword

/**
 * The layout of entering a password, shared by the unlock and the locked screens - one layout, two
 * processors (pass-docs, `decisions.md`, "`unlock` и `locked` — два экрана"). No state of its own.
 *
 * @param databaseName the name of the database, shown as the title
 * @param abilities what the screen lets do besides the password
 * @param read the state is read: until then the shield alone, large, in the middle - not the defaults drawn
 * as the database; once read the shield shrinks and the rest opens out under it, so the shield slides to its
 * place (pass-docs, Step 72, fix 4)
 */
@Composable
internal fun PasswordEntryActions.PasswordEntryContent(
    databaseName: String,
    entry: PasswordEntry,
    submitText: String,
    abilities: PasswordEntryAbilities,
    read: Boolean,
    subtitle: String? = null,
) {
    val shieldSize by animateDpAsState(
        targetValue = if (read) SHIELD_SIZE else SHIELD_SIZE_READING,
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
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    DatabaseName(name = databaseName, changeable = abilities.changeDatabase, enabled = !entry.busy)
                    Spacer(Modifier.height(32.dp))

                    // A problem with the file takes the place of the password - the one fades into the other
                    AnimatedContent(
                        targetState = entry,
                        contentKey = { it.problem.hidesPassword },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "file problem",
                    ) { shown ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (shown.problem.hidesPassword) {
                                FileProblem(entry = shown, abilities = abilities)
                            } else {
                                PasswordInput(entry = shown, submitText = submitText)
                                BiometricSignInRow(entry = shown)
                                Spacer(Modifier.height(16.dp))
                                KeyFileRow(entry = shown, changeable = abilities.changeKeyFile)
                                if (abilities.createDatabase) {
                                    Spacer(Modifier.height(24.dp))
                                    CreateDatabaseButton(enabled = !shown.busy)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The shield over the password: smaller than on the welcome screen, which has nothing else to show. */
private val SHIELD_SIZE = 96.dp

/** The shield alone while the state is read - as large as on the welcome screen. */
private val SHIELD_SIZE_READING = 140.dp

/** The shield shrinking and the rest opening out under it - one length for both, so they go as one. */
private const val READ_MILLIS = 400
