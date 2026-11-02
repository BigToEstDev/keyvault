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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.settings.lock.AutoLockTimeout
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.biometric.BiometricPurpose
import ru.kino.dev.ui.biometric.rememberBiometricPrompt
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.TextInputDialog
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.WheelPickerDialog
import ru.kino.dev.ui.res.stringResource

/**
 * How the database locks and how one gets in: the group "Locking" - the auto-lock, locking on leaving the app,
 * the lock button; the group "Sign-in" - biometrics of the database, asking for them by themselves, wiping a wrong
 * password (pass-docs, Step 59).
 */
@Composable
internal fun LockScreen(
    dbKey: String,
    processor: LockProcessor = koinViewModel { parametersOf(dbKey) },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val showPrompt = rememberBiometricPrompt()

    processor.collectSideEffect { effect ->
        when (effect) {
            is LockSideEffect.ShowBiometricPrompt -> showPrompt(effect.session.cipher, BiometricPurpose.Enable) {
                processor.onBiometricPromptFinished(effect.session, it)
            }
        }
    }

    LaunchedEffect(Unit) { processor.onLoad() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                LockMessage.NotSaved -> Res.string.settings_message_not_saved
                LockMessage.BiometricFailed -> Res.string.settings_message_biometric_failed
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_lock),
                    onBack = processor::onBack,
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
                item(key = "locking") { SettingsSectionHeader(title = stringResource(Res.string.settings_lock_group)) }
                item(key = "auto-lock") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ValueRow(
                            title = stringResource(Res.string.settings_auto_lock),
                            text = stringResource(Res.string.settings_auto_lock_hint),
                            // Until read nothing is chosen for real: the row is disabled meanwhile
                            value = state.timeout?.let { timeoutLabel(it) }.orEmpty(),
                            onClick = processor::onChooseTimeout,
                            enabled = state.timeout != null,
                        )
                    }
                }
                item(key = "lock-on-leave") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(Res.string.settings_lock_on_leave),
                            text = stringResource(Res.string.settings_lock_on_leave_hint),
                            checked = state.lockOnLeave ?: false,
                            onCheckedChange = processor::onSetLockOnLeave,
                            enabled = state.lockOnLeave != null,
                        )
                    }
                }
                item(key = "lock-button") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(Res.string.settings_lock_button),
                            text = stringResource(Res.string.settings_lock_button_hint),
                            checked = state.lockButton ?: false,
                            onCheckedChange = processor::onSetLockButton,
                            enabled = state.lockButton != null,
                        )
                    }
                }
                item(key = "sign-in") {
                    // Set apart from the group above, as the sections of the main screen
                    SettingsSectionHeader(title = stringResource(Res.string.settings_sign_in_group), top = 8.dp)
                }
                item(key = "biometric") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        val row = state.biometric
                        SwitchRow(
                            title = stringResource(Res.string.settings_biometric),
                            text = stringResource(
                                when (row?.unavailable) {
                                    BiometricUnavailable.Unsupported -> Res.string.settings_biometric_unsupported
                                    BiometricUnavailable.NoHardware -> Res.string.settings_biometric_no_hardware
                                    BiometricUnavailable.NoPassword -> Res.string.settings_biometric_no_password
                                    null -> Res.string.settings_biometric_hint
                                },
                            ),
                            checked = row?.on ?: false,
                            onCheckedChange = processor::onSetBiometric,
                            // On, it can always be turned off - forgetting needs neither the sensor nor a password
                            enabled = row != null && (row.unavailable == null || row.on),
                        )
                    }
                }
                item(key = "auto-biometric-prompt") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(Res.string.settings_auto_biometric_prompt),
                            text = stringResource(Res.string.settings_auto_biometric_prompt_hint),
                            checked = state.autoBiometricPromptAllowed && state.autoBiometricPrompt == true,
                            onCheckedChange = processor::onSetAutoBiometricPrompt,
                            enabled = state.autoBiometricPromptAllowed && state.autoBiometricPrompt != null,
                        )
                    }
                }
                item(key = "clear-password") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(Res.string.settings_clear_password),
                            text = stringResource(Res.string.settings_clear_password_hint),
                            checked = state.clearPasswordOnError ?: false,
                            onCheckedChange = processor::onSetClearPasswordOnError,
                            enabled = state.clearPasswordOnError != null,
                        )
                    }
                }
            }
        }
    }

    state.biometricPassword?.let { dialog ->
        TextInputDialog(
            title = stringResource(Res.string.settings_biometric_password_title),
            value = dialog.password,
            onValueChange = processor::onBiometricPasswordChange,
            label = stringResource(Res.string.entry_field_password),
            confirmText = stringResource(Res.string.action_next),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = processor::onBiometricPasswordConfirm,
            onDismiss = processor::onBiometricPasswordDismiss,
            error = if (dialog.wrong) stringResource(Res.string.settings_biometric_wrong_password) else null,
            loading = dialog.checking,
            secret = true,
        )
    }

    if (state.forgettingBiometric) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_biometric_forget_title),
            text = stringResource(Res.string.settings_biometric_forget_text),
            confirmText = stringResource(Res.string.settings_biometric_forget),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = processor::onBiometricForgetConfirm,
            onDismiss = processor::onBiometricForgetDismiss,
        )
    }

    val timeout = state.timeout
    if (state.choosingTimeout && timeout != null) {
        WheelPickerDialog(
            title = stringResource(Res.string.settings_auto_lock),
            options = AutoLockTimeout.entries,
            selected = timeout,
            label = { timeoutLabel(it) },
            onConfirm = processor::onSelectTimeout,
            onDismiss = processor::onDismissTimeout,
        )
    }
}

/** "30 s", "5 min" - short, the value at the end of a row and a choice on the wheel. */
@Composable
private fun timeoutLabel(timeout: AutoLockTimeout): String {
    val seconds = timeout.duration.inWholeSeconds.toInt()
    return if (seconds < SECONDS_IN_MINUTE) {
        stringResource(Res.string.settings_seconds_short, seconds)
    } else {
        stringResource(Res.string.settings_minutes_short, seconds / SECONDS_IN_MINUTE)
    }
}

private const val SECONDS_IN_MINUTE = 60
