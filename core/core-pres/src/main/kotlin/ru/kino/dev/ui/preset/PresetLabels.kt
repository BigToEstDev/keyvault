package ru.kino.dev.ui.preset

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.StandardPreset

/*
 * How presets are named on the screens - of entries in `vault`, of presets in `settings` (pass-docs, Step 62).
 */

/** The name of [preset]: one's own is named by the person, the standard one - from the resources. */
@Composable
fun presetName(preset: EntryPreset): String = preset.name ?: stringResource(R.string.entry_preset_standard)

/** The label of a field of the standard preset; an own field is labelled with its key. */
@Composable
fun fieldLabel(key: String): String = when (key) {
    StandardPreset.TITLE -> stringResource(R.string.entry_field_title)
    StandardPreset.USER_NAME -> stringResource(R.string.entry_field_user_name)
    StandardPreset.PASSWORD -> stringResource(R.string.entry_field_password)
    StandardPreset.SERVICE -> stringResource(R.string.entry_field_service)
    StandardPreset.URL -> stringResource(R.string.entry_field_url)
    StandardPreset.NOTES -> stringResource(R.string.entry_field_notes)
    else -> key
}
