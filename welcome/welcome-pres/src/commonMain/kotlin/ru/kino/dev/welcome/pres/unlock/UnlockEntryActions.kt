package ru.kino.dev.welcome.pres.unlock

import ru.kino.dev.welcome.pres.common.passwordentry.PasswordEntryActions

/** [PasswordEntryActions] of the unlock screen: each told to its [processor]. */
internal class UnlockEntryActions(private val processor: UnlockProcessor) : PasswordEntryActions {

    override fun onPasswordChange(password: String) {
        processor.action(UnlockAction.PasswordChanged(password))
    }

    override fun onTogglePasswordVisible() {
        processor.action(UnlockAction.TogglePasswordVisible)
    }

    override fun onSubmit() {
        processor.action(UnlockAction.Submit)
    }

    override fun onRetry() {
        processor.action(UnlockAction.Retry)
    }

    override fun onAttachKeyFile() {
        processor.action(UnlockAction.AttachKeyFile)
    }

    override fun onRemoveKeyFile() {
        processor.action(UnlockAction.RemoveKeyFile)
    }

    override fun onBiometric() {
        processor.action(UnlockAction.Biometric)
    }

    override fun onChangeDatabase() {
        processor.action(UnlockAction.ChangeDatabase)
    }

    override fun onCreateDatabase() {
        processor.action(UnlockAction.CreateDatabase)
    }
}
