package ru.kino.dev.welcome.pres.keyfilebackup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.GradientBackground

/** The key file of a new database in one column on the backdrop, as on a phone. */
@Composable
internal fun KeyFileBackupActions.KeyFileBackupMobile(state: KeyFileBackupState) {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            KeyFileBackupForm(state)
        }
    }
}
