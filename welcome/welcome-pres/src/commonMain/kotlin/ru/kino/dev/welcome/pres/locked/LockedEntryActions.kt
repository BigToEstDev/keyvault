package ru.kino.dev.welcome.pres.locked

import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryAbilities
import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryActions

/**
 * [PasswordEntryActions] of the locked screen: each told to its [processor]. Changing the database or its key file
 * is not done here - the database is open ([PasswordEntryAbilities.Locked]), so those are never asked.
 */
internal class LockedEntryActions(private val processor: LockedProcessor) : PasswordEntryActions {

    override fun onPasswordChange(password: String) {
        processor.action(LockedAction.PasswordChanged(password))
    }

    override fun onTogglePasswordVisible() {
        processor.action(LockedAction.TogglePasswordVisible)
    }

    override fun onSubmit() {
        processor.action(LockedAction.Submit)
    }

    // The file is read again by the next try itself
    override fun onRetry() {
        processor.action(LockedAction.Submit)
    }

    override fun onAttachKeyFile() {
        processor.action(LockedAction.AttachKeyFile)
    }

    override fun onBiometric() {
        processor.action(LockedAction.Biometric)
    }

    override fun onRemoveKeyFile() = Unit

    override fun onChangeDatabase() = Unit

    override fun onCreateDatabase() = Unit
}
