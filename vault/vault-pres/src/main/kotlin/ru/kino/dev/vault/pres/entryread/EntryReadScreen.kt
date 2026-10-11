package ru.kino.dev.vault.pres.entryread

import androidx.compose.runtime.Composable
import ru.kino.dev.vault.pres.common.EntryStub

/** Reading an entry - a stub: nothing leads here until the entries are on the list (pass-docs, Step 38). */
@Composable
internal fun EntryReadScreen(onBack: () -> Unit) {
    EntryStub(onBack = onBack)
}
