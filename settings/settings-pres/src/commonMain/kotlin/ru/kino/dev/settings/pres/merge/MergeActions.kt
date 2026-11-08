package ru.kino.dev.settings.pres.merge

import ru.kino.dev.vault.sync.OtherDatabaseKind

/** What the page `Синхронизация` asks of its processor (pass-docs, Step 108, 111). */
interface MergeActions {

    fun onBack()

    /**
     * The file picked on the card of [kind] - "Merge with a copy", "Synchronize with a copy" (both ways) or "Import
     * another database".
     */
    fun onDatabasePicked(kind: OtherDatabaseKind, uri: String)

    /** Typed into the password of the other database. */
    fun onPasswordChange(password: String)

    fun onTogglePasswordVisible()

    /** The key file of the other database, picked. */
    fun onKeyFilePicked(uri: String)

    fun onRemoveKeyFile()

    /** "Merge" / "Import" in the dialog of the credentials. */
    fun onConfirm()

    /** "Cancel" in the dialog of the credentials - not while the merge runs. */
    fun onDismissCredentials()

    /** "OK" in the dialog of what came in, or of what failed. */
    fun onDismissMessage()

    /** "Save a copy": the file to save the copy into is asked, the name of this database's file suggested (Step 111). */
    fun onSaveCopy()

    /** The file picked to save the copy into. */
    fun onCopyTargetPicked(uri: String)

    /** "Send": the file of this database handed to the share menu of the system - android only (Step 111). */
    fun onShare()
}
