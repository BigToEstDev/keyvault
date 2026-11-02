package ru.kino.dev.settings.pres.appearance

import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode

/**
 * @param themeMode the chosen mode; null until read - its choice is disabled
 * @param language the chosen language; null until read - its choice is disabled
 * @param passwordColoring whether passwords are colored; null until read - its choice is disabled
 * @param passwordStrength whether a password made shows its strength; null until read - its choice is disabled
 * @param userNameInList whether the list shows user names; null until read - its choice is disabled
 * @param entryCount whether groups show their number; null until read - its choice is disabled
 * @param entryCountNested whether the number counts the groups below; null until read - its choice is disabled
 * @param userNameHidden the open database hides user names: "user name in the list" is off and takes nothing
 * @param message for the snackbar, until it takes it
 */
data class AppearanceState(
    val themeMode: ThemeMode? = null,
    val language: AppLanguage? = null,
    val passwordColoring: Boolean? = null,
    val passwordStrength: Boolean? = null,
    val userNameInList: Boolean? = null,
    val entryCount: Boolean? = null,
    val entryCountNested: Boolean? = null,
    val userNameHidden: Boolean = false,
    val message: AppearanceMessage? = null,
)

/** What the snackbar says. */
enum class AppearanceMessage {
    /** The choice could not be written: it is back to what it was. */
    NotSaved,
}
