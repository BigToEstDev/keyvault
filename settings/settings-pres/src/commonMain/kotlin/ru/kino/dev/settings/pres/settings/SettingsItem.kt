package ru.kino.dev.settings.pres.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import ru.kino.dev.core.pres.*

/** Where a setting is kept: on this device, or in the database file - the two sections of the screen. */
enum class SettingsSection(val title: StringResource, val hint: StringResource) {
    App(Res.string.settings_section_app, Res.string.settings_section_app_hint),
    Database(Res.string.settings_section_database, Res.string.settings_section_database_hint),
}

/** A card of the settings screen, in the order of the screen (pass-docs, `settings/settings.md`). */
enum class SettingsItem(
    val section: SettingsSection,
    val icon: ImageVector,
    val title: StringResource,
    val text: StringResource,
) {
    Appearance(SettingsSection.App, Icons.Filled.Palette, Res.string.settings_appearance, Res.string.settings_appearance_text),
    Lock(SettingsSection.App, Icons.Filled.Lock, Res.string.settings_lock, Res.string.settings_lock_text),
    Privacy(SettingsSection.App, Icons.Filled.Shield, Res.string.settings_privacy, Res.string.settings_privacy_text),
    Presets(SettingsSection.App, Icons.Filled.Dashboard, Res.string.settings_presets, Res.string.settings_presets_text),
    File(SettingsSection.Database, Icons.Filled.Description, Res.string.settings_file, Res.string.settings_file_text),
    Entries(SettingsSection.Database, Icons.AutoMirrored.Filled.ListAlt, Res.string.settings_entries, Res.string.settings_entries_text),
    Security(SettingsSection.Database, Icons.Filled.Key, Res.string.settings_security, Res.string.settings_security_text),
    Sync(SettingsSection.Database, Icons.Filled.Sync, Res.string.settings_sync, Res.string.settings_sync_text),
}
