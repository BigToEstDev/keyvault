package ru.kino.dev.welcome.pres.common.passwordentry

import androidx.compose.runtime.Composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType
import ru.kino.dev.welcome.pres.common.PasswordEntry

/**
 * Entering a password, shared by the unlock and the locked screens - one layout, two processors (pass-docs,
 * `decisions.md`, "`unlock` и `locked` — два экрана"). No state of its own; here only the layout is picked by the
 * width of the window (Step 94): [PasswordEntryMobile] or [PasswordEntryTablet].
 *
 * @param databaseName the name of the database, shown as the title
 * @param abilities what the screen lets do besides the password
 * @param read the state is read: until then the shield alone - not the defaults drawn as the database
 * (pass-docs, Step 72, fix 4)
 */
@Composable
internal fun PasswordEntryActions.PasswordEntryContent(
    databaseName: String,
    entry: PasswordEntry,
    submitText: String,
    abilities: PasswordEntryAbilities,
    read: Boolean,
    subtitle: String? = null,
) {
    when (LocalLayoutType.current) {
        LayoutType.Mobile -> PasswordEntryMobile(databaseName, entry, submitText, abilities, read, subtitle)
        LayoutType.Tablet -> PasswordEntryTablet(databaseName, entry, submitText, abilities, read, subtitle)
    }
}
