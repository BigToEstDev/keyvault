package ru.kino.dev.welcome.pres.masterpassword

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberCreateDocument
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.OverwriteDialog

@Composable
internal fun MasterPasswordScreen(
    draft: CreateDraft,
    processor: MasterPasswordProcessor = koinViewModel { parametersOf(draft) },
) {
    val state by processor.collectAsState()
    val pickDatabase = rememberCreateDocument(DocumentKind.Database) { processor.onDatabasePicked(it) }

    processor.collectSideEffect { effect ->
        when (effect) {
            is MasterPasswordSideEffect.PickDatabase -> pickDatabase(effect.suggestedName)
        }
    }

    state.stage.overwrite?.let {
        OverwriteDialog(
            overwrite = it,
            onConfirm = processor::onOverwriteConfirmed,
            onDismiss = processor::onOverwriteDismissed,
        )
    }

    processor.MasterPasswordContent(state)
}
