package ru.kino.dev.settings.pres.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import ru.kino.dev.core.pres.R

/** Where a setting is kept: on this device, or in the database file - the two sections of the screen. */
enum class SettingsSection(@param:StringRes val title: Int, @param:StringRes val hint: Int) {
    App(R.string.settings_section_app, R.string.settings_section_app_hint),
    Database(R.string.settings_section_database, R.string.settings_section_database_hint),
}

/** A card of the settings screen, in the order of the screen (pass-docs, `settings/settings.md`). */
enum class SettingsItem(
    val section: SettingsSection,
    val icon: ImageVector,
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
) {
    Appearance(SettingsSection.App, Icons.Filled.Palette, R.string.settings_appearance, R.string.settings_appearance_text),
    Lock(SettingsSection.App, Icons.Filled.Lock, R.string.settings_lock, R.string.settings_lock_text),
    Privacy(SettingsSection.App, Icons.Filled.Shield, R.string.settings_privacy, R.string.settings_privacy_text),
    Presets(SettingsSection.App, Icons.Filled.Dashboard, R.string.settings_presets, R.string.settings_presets_text),
    File(SettingsSection.Database, Icons.Filled.Description, R.string.settings_file, R.string.settings_file_text),
    Entries(SettingsSection.Database, Icons.AutoMirrored.Filled.ListAlt, R.string.settings_entries, R.string.settings_entries_text),
    RecycleBin(SettingsSection.Database, Icons.Filled.Delete, R.string.settings_recycle_bin, R.string.settings_recycle_bin_text),
    Security(SettingsSection.Database, Icons.Filled.Key, R.string.settings_security, R.string.settings_security_text),
}
