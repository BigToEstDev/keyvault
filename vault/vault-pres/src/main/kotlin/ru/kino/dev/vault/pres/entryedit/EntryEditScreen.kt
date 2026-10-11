package ru.kino.dev.vault.pres.entryedit

import androidx.compose.runtime.Composable
import ru.kino.dev.vault.pres.common.EntryStub

/** Creating or editing an entry - a stub, reached from "Create entry" (pass-docs, Step 38). */
@Composable
internal fun EntryEditScreen(onBack: () -> Unit) {
    EntryStub(onBack = onBack)
}
