package ru.kino.dev.ui.components

import androidx.compose.runtime.Composable

/**
 * Keeps the window of the app out of autofill: no service offers or saves what is typed in it (pass-docs, Step 74,
 * `settings/decisions.md`, "Защита данных"). Nothing to do where the system has no autofill - the desktop.
 */
@Composable
internal expect fun ExcludeWindowFromAutofill()
