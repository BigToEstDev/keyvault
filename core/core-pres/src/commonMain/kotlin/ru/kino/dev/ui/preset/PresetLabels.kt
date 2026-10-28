package ru.kino.dev.ui.preset

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.res.stringResource

/*
 * How presets are named on the screens - of entries in `vault`, of presets in `settings` (pass-docs, Step 62).
 */

/** The name of [preset]: one's own is named by the person, the standard one - from the resources. */
@Composable
fun presetName(preset: EntryPreset): String = preset.name ?: stringResource(Res.string.entry_preset_standard)

/** The label of a field of the standard preset; an own field is labelled with its key. */
@Composable
fun fieldLabel(key: String): String = when (key) {
    StandardPreset.TITLE -> stringResource(Res.string.entry_field_title)
    StandardPreset.USER_NAME -> stringResource(Res.string.entry_field_user_name)
    StandardPreset.PASSWORD -> stringResource(Res.string.entry_field_password)
    StandardPreset.SERVICE -> stringResource(Res.string.entry_field_service)
    StandardPreset.URL -> stringResource(Res.string.entry_field_url)
    StandardPreset.NOTES -> stringResource(Res.string.entry_field_notes)
    else -> key
}
