package ru.kino.dev.settings.pres.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.WheelPickerDialog

/**
 * How the database locks and how one gets in: the group "Locking" - the auto-lock, locking on leaving the app,
 * the lock button; the group "Sign-in" - wiping a wrong password (pass-docs, Step 59).
 */
@Composable
internal fun LockScreen(processor: LockProcessor = hiltViewModel()) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.action(LockAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                LockMessage.NotSaved -> R.string.settings_message_not_saved
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(LockAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_lock),
                    onBack = { processor.action(LockAction.Back) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "locking") { SettingsSectionHeader(title = stringResource(R.string.settings_lock_group)) }
                item(key = "auto-lock") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ValueRow(
                            title = stringResource(R.string.settings_auto_lock),
                            text = stringResource(R.string.settings_auto_lock_hint),
                            // Until read nothing is chosen for real: the row is disabled meanwhile
                            value = state.timeout?.let { timeoutLabel(it) }.orEmpty(),
                            onClick = { processor.action(LockAction.ChooseTimeout) },
                            enabled = state.timeout != null,
                        )
                    }
                }
                item(key = "lock-on-leave") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_lock_on_leave),
                            text = stringResource(R.string.settings_lock_on_leave_hint),
                            checked = state.lockOnLeave ?: false,
                            onCheckedChange = { processor.action(LockAction.SetLockOnLeave(it)) },
                            enabled = state.lockOnLeave != null,
                        )
                    }
                }
                item(key = "lock-button") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_lock_button),
                            text = stringResource(R.string.settings_lock_button_hint),
                            checked = state.lockButton ?: false,
                            onCheckedChange = { processor.action(LockAction.SetLockButton(it)) },
                            enabled = state.lockButton != null,
                        )
                    }
                }
                item(key = "sign-in") {
                    // Set apart from the group above, as the sections of the main screen
                    SettingsSectionHeader(title = stringResource(R.string.settings_sign_in_group), top = 8.dp)
                }
                item(key = "clear-password") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_clear_password),
                            text = stringResource(R.string.settings_clear_password_hint),
                            checked = state.clearPasswordOnError ?: false,
                            onCheckedChange = { processor.action(LockAction.SetClearPasswordOnError(it)) },
                            enabled = state.clearPasswordOnError != null,
                        )
                    }
                }
            }
        }
    }

    val timeout = state.timeout
    if (state.choosingTimeout && timeout != null) {
        WheelPickerDialog(
            title = stringResource(R.string.settings_auto_lock),
            options = AutoLockTimeout.entries,
            selected = timeout,
            label = { timeoutLabel(it) },
            onConfirm = { processor.action(LockAction.SelectTimeout(it)) },
            onDismiss = { processor.action(LockAction.DismissTimeout) },
        )
    }
}

/** "30 s", "5 min" - short, the value at the end of a row and a choice on the wheel. */
@Composable
private fun timeoutLabel(timeout: AutoLockTimeout): String {
    val seconds = timeout.duration.inWholeSeconds.toInt()
    return if (seconds < SECONDS_IN_MINUTE) {
        stringResource(R.string.settings_seconds_short, seconds)
    } else {
        stringResource(R.string.settings_minutes_short, seconds / SECONDS_IN_MINUTE)
    }
}

private const val SECONDS_IN_MINUTE = 60
