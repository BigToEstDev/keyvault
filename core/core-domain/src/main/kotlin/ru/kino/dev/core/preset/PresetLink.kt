package ru.kino.dev.core.preset

/**
 * The link from an entry to its preset of one's own (pass-docs, Step 44, answer 3): a plain string of the
 * entry, under [KEY], holding the [PresetId]. The description of the preset lives in the app, not in the
 * file - other programs see the link as one more field.
 *
 * An entry of the standard preset has none. The screens never show it; no field of one's own takes its name.
 */
object PresetLink {

    /** The key of the link - never changed: entries already written carry it. */
    const val KEY = "KeyVaultPreset"
}
