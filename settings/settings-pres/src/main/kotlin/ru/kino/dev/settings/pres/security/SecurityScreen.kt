package ru.kino.dev.settings.pres.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.KdfVariant
import ru.kino.dev.core.pres.*
import ru.kino.dev.settings.pres.entries.MEGABYTE
import ru.kino.dev.settings.pres.security.components.CredentialCard
import ru.kino.dev.settings.pres.security.components.PasswordDialog
import ru.kino.dev.settings.pres.security.components.SecuritySkeleton
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.ItemMenu
import ru.kino.dev.ui.components.MenuItem
import ru.kino.dev.ui.components.RadioGroup
import ru.kino.dev.ui.components.Reveal
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.ValueRow
import ru.kino.dev.ui.components.WheelPickerDialog
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.files.rememberOpenDocument
import ru.kino.dev.ui.res.stringResource

/**
 * The security of the database: the credentials, the cipher, the key derivation and its rounds with the open time
 * on this phone (pass-docs, Step 67).
 */
@Composable
internal fun SecurityScreen(
    dbKey: String,
    processor: SecurityProcessor = koinViewModel { parametersOf(dbKey) },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.action(SecurityAction.Load) }

    val message = state.message?.let { messageText(it) }
    SnackbarMessage(message, snackbar) { processor.action(SecurityAction.MessageShown) }
    val pickKeyFile = rememberOpenDocument { processor.action(SecurityAction.KeyFilePicked(it)) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_security),
                    onBack = { processor.action(SecurityAction.Back) },
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
                val security = state.security
                if (security == null) {
                    // Until read there is nothing to show for real: the shapes of the screen
                    item(key = "skeleton") { SecuritySkeleton(skeletonBrush()) }
                } else {
                    credentialRows(security, state.saving, pickKeyFile, processor::action)
                    encryptionRows(security, state.kdfPass, state.saving, processor::action)
                }
            }
        }
    }

    if (state.choosingRounds) {
        state.security?.let { RoundsDialog(it, state.kdfPass, processor::action) }
    }
    state.passwordDialog?.let { PasswordDialog(it, processor::action) }
    if (state.removingKeyFile) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_security_key_file_remove_title),
            text = stringResource(Res.string.settings_security_key_file_remove_text),
            confirmText = stringResource(Res.string.action_remove),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = { processor.action(SecurityAction.RemoveKeyFileConfirm) },
            onDismiss = { processor.action(SecurityAction.RemoveKeyFileDismiss) },
            destructive = true,
        )
    }
}

private fun LazyListScope.credentialRows(
    security: DatabaseSecurity,
    saving: SecurityPart?,
    pickKeyFile: () -> Unit,
    action: (SecurityAction) -> Unit,
) {
    item(key = "credentials") {
        SettingsSectionHeader(title = stringResource(Res.string.settings_security_credentials_group))
    }
    item(key = "password") {
        CredentialCard(
            title = stringResource(Res.string.settings_security_password),
            value = stringResource(
                if (security.credentials.password) {
                    Res.string.settings_security_password_set
                } else {
                    Res.string.settings_security_password_none
                },
            ),
            hint = stringResource(Res.string.settings_security_password_hint),
            onClick = { action(SecurityAction.EditPassword) },
            enabled = saving == null,
        )
    }
    item(key = "key_file") { KeyFileCard(security, saving, pickKeyFile, action) }
}

private fun LazyListScope.encryptionRows(
    security: DatabaseSecurity,
    kdfPass: KdfPass,
    saving: SecurityPart?,
    action: (SecurityAction) -> Unit,
) {
    val encryption = security.encryption
    item(key = "encryption") {
        // Set apart from the group above, as the sections of the main screen
        SettingsSectionHeader(
            title = stringResource(Res.string.settings_security_encryption_group),
            hint = stringResource(Res.string.settings_security_encryption_hint),
            top = 8.dp,
        )
    }
    item(key = "cipher") {
        ChoiceCard(saving == SecurityPart.Cipher) {
            RadioGroup(
                title = stringResource(Res.string.settings_security_cipher),
                options = listOf(Cipher.Aes256, Cipher.ChaCha20),
                selected = encryption.cipher,
                label = { cipherLabel(it) },
                onSelect = { action(SecurityAction.SelectCipher(it)) },
                enabled = saving == null,
            )
        }
    }
    item(key = "kdf") {
        ChoiceCard(saving == SecurityPart.Kdf) {
            RadioGroup(
                title = stringResource(Res.string.settings_security_kdf),
                options = listOf(KdfVariant.Argon2id, KdfVariant.Argon2d),
                selected = encryption.kdf,
                label = { kdfLabel(it) },
                onSelect = { action(SecurityAction.SelectKdf(it)) },
                enabled = saving == null,
            )
        }
    }
    item(key = "kdf_parameters") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                // As the file has them, 20 of a database from elsewhere too: the wheel offers ours. The open time
                // opens out under the title once measured - no skeleton, the card does not jump (pass-docs, Step 72)
                val rounds = encryption.iterations
                ValueRow(
                    title = stringResource(Res.string.settings_security_rounds),
                    value = rounds.toString(),
                    onClick = { action(SecurityAction.ChooseRounds) },
                    text = (kdfPass as? KdfPass.Measured)?.let { openTime(it, rounds) },
                    enabled = saving == null,
                )
                if (saving == SecurityPart.Rounds) SavingProgress(Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                // What makes guessing expensive stays as the file has it: to read only
                ValueRow(
                    title = stringResource(Res.string.settings_security_memory),
                    value = stringResource(Res.string.settings_megabytes_short, megabytes(security.memoryBytes)),
                    onClick = null,
                )
                ValueRow(
                    title = stringResource(Res.string.settings_security_parallelism),
                    value = security.parallelism.toString(),
                    onClick = null,
                )
            }
        }
    }
}

/**
 * The key file: none - a tap picks the first one; one - a tap opens "Change" and, with a password, "Remove". Without
 * a password the key file is all that opens the database, and it cannot go.
 */
@Composable
private fun KeyFileCard(
    security: DatabaseSecurity,
    saving: SecurityPart?,
    pickKeyFile: () -> Unit,
    action: (SecurityAction) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val hasKeyFile = security.credentials.keyFile
    CredentialCard(
        title = stringResource(Res.string.key_file_title),
        value = security.keyFileName ?: stringResource(Res.string.settings_security_key_file_none),
        hint = stringResource(Res.string.settings_security_key_file_hint),
        onClick = { if (hasKeyFile) menu = true else pickKeyFile() },
        enabled = saving == null,
        footer = {
            if (saving == SecurityPart.KeyFile) SavingProgress(Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        },
    ) {
        ItemMenu(
            expanded = menu,
            onDismiss = { menu = false },
            items = buildList {
                add(MenuItem(stringResource(Res.string.action_change), Icons.Filled.SwapHoriz, pickKeyFile))
                if (security.credentials.password) {
                    add(MenuItem(stringResource(Res.string.action_remove), Icons.Filled.LinkOff) {
                        action(SecurityAction.RemoveKeyFile)
                    })
                }
            },
            offset = DpOffset(x = 16.dp, y = 0.dp),
        )
    }
}

@Composable
private fun messageText(message: SecurityMessage): String = when (message) {
    SecurityMessage.NotSaved -> stringResource(Res.string.message_file_not_saved)
    SecurityMessage.Failed -> stringResource(Res.string.error_unexpected)
    SecurityMessage.PasswordSaved -> stringResource(Res.string.settings_security_password_saved)
    SecurityMessage.PasswordSavedBiometricsOff -> stringResource(Res.string.settings_security_password_saved_biometrics_off)
    SecurityMessage.KeyFileSaved -> stringResource(Res.string.settings_security_key_file_saved)
    SecurityMessage.KeyFileRemoved -> stringResource(Res.string.settings_security_key_file_removed)
    SecurityMessage.KeyFileTooLarge ->
        stringResource(Res.string.error_key_file_too_large, (CredentialLimits.KEY_FILE_MAX_SIZE / MEGABYTE).toInt())
    SecurityMessage.KeyFileUnreadable -> stringResource(Res.string.error_key_file_unreachable)
    SecurityMessage.KeyFileNotRemembered -> stringResource(Res.string.settings_security_key_file_not_remembered)
}

/** A choice of the encryption in a card, with progress under it while the file is written with it. */
@Composable
private fun ChoiceCard(saving: Boolean, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
            if (saving) SavingProgress(Modifier.padding(top = 12.dp))
        }
    }
}

/** While the file is written - the key derivation takes seconds. */
@Composable
private fun SavingProgress(modifier: Modifier) {
    LinearProgressIndicator(modifier = modifier.fillMaxWidth())
}

@Composable
private fun RoundsDialog(security: DatabaseSecurity, kdfPass: KdfPass, action: (SecurityAction) -> Unit) {
    WheelPickerDialog(
        title = stringResource(Res.string.settings_security_rounds),
        options = RoundsChoices.OPTIONS,
        selected = RoundsChoices.onWheel(security.encryption.iterations),
        label = { it.toString() },
        onConfirm = { action(SecurityAction.SelectRounds(it)) },
        onDismiss = { action(SecurityAction.DismissRounds) },
        confirmText = stringResource(Res.string.action_save),
    ) { rounds ->
        Column(modifier = Modifier.padding(top = 16.dp)) {
            // Opens out once measured, as on the card, the gap with it (pass-docs, Step 72)
            Reveal(kdfPass as? KdfPass.Measured) {
                Text(
                    text = openTime(it, rounds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = stringResource(Res.string.settings_security_rounds_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Opening on this phone: about 2.6 s" - one pass times [rounds], in the way of the language. */
@Composable
private fun openTime(kdfPass: KdfPass.Measured, rounds: Long): String {
    val seconds = kdfPass.pass.inWholeMilliseconds * rounds / MILLIS_IN_SECOND
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    return stringResource(Res.string.settings_security_open_time, number.format(seconds))
}

/** The memory of Argon2 as KeePass shows it, in MiB - "64"; a size that is no whole one - "3.5". */
private fun megabytes(bytes: Long): String {
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    return number.format(bytes.toDouble() / MEGABYTE)
}

@Composable
private fun cipherLabel(cipher: Cipher): String = stringResource(
    when (cipher) {
        Cipher.Aes256 -> Res.string.settings_security_cipher_aes
        Cipher.ChaCha20 -> Res.string.settings_security_cipher_chacha
    },
)

@Composable
private fun kdfLabel(kdf: KdfVariant): String = stringResource(
    when (kdf) {
        KdfVariant.Argon2id -> Res.string.settings_security_kdf_argon2id
        KdfVariant.Argon2d -> Res.string.settings_security_kdf_argon2d
    },
)

private const val MILLIS_IN_SECOND = 1000.0
