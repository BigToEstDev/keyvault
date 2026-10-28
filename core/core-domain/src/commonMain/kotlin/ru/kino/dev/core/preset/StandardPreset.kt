package ru.kino.dev.core.preset

/**
 * The preset every entry is shown with when it is of no preset of one's own: the kdbx standard fields and
 * the name of the service, in one card. Decided by the owner, Step 43; the order is the order of the form.
 * It is neither changed nor deleted (pass-docs, `vault-our.md`).
 */
object StandardPreset {

    const val TITLE = "Title"
    const val USER_NAME = "UserName"
    const val PASSWORD = "Password"
    const val SERVICE = "Service"
    const val URL = "URL"
    const val NOTES = "Notes"

    val preset = EntryPreset(
        id = null,
        name = null,
        cards = listOf(
            PresetCard(
                listOf(
                    PresetField(USER_NAME, required = true, writtenWhenEmpty = true),
                    PresetField(PASSWORD, required = true, protected = true, writtenWhenEmpty = true),
                    PresetField(SERVICE),
                    PresetField(URL, writtenWhenEmpty = true),
                    PresetField(NOTES, writtenWhenEmpty = true),
                ),
            ),
        ),
    )
}
