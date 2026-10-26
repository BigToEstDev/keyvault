package ru.kino.dev.vault.pres.common

import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneType
import ru.kino.dev.vault.entry.EntryContent

/** The countries of the phone fields of [content] read ahead, so a screen draws them without waiting (pass-docs, Step 77). */
internal suspend fun PhoneNumbers.prepareFor(content: EntryContent) {
    prepare((content.presetFields + content.ownFields).mapNotNull { PhoneType.region(it.type) }.toSet())
}
