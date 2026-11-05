package ru.kino.dev.vault.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.expectFailure
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.NewAttachment
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.GroupId

class EntryVersionOpenerTest {

    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN")))
    private val entries = FakeEntryRepository()
    private val opener =
        EntryVersionOpener(FakeEntryHistoryRepository(entries), EntryOpener(entries, FakePresetRepository(card)))
    private val mail = entries.add("mail", GroupId("work"), listOf(field(TITLE, "Mail")), createdAt = 1_000L)

    @Test
    fun `a version opens as it was, with when it was saved`() = runTest {
        entries.update(DB, mail, listOf(field(TITLE, "Post")))

        val version = opener.open(DB, mail, 0)

        assertEquals("Mail", version.content.value(TITLE))
        assertEquals(StandardPreset.preset, version.content.preset)
        assertEquals(1_000L, version.times.modified)
    }

    @Test
    fun `a version opens with its attachments then, not the entry's now`() = runTest {
        entries.update(DB, mail, listOf(field(TITLE, "Mail")), AttachmentsEdit(emptyList(), listOf(file("old.txt"))))
        entries.update(DB, mail, listOf(field(TITLE, "Mail")), AttachmentsEdit(emptyList(), listOf(file("new.txt"))))

        val version = opener.open(DB, mail, 1)

        assertEquals(listOf("old.txt"), version.attachments.map { it.name })
    }

    @Test
    fun `a version is laid over the preset it linked to then, not the one of the entry now`() = runTest {
        val then = listOf(field(TITLE, "Visa"), field("PIN", "1"), field(PresetLink.KEY, "card"))
        entries.stored[mail] = entries.stored.getValue(mail).copy(
            history = listOf(FakeEntryRepository.StoredVersion(then, savedAt = 900L)),
        )

        val version = opener.open(DB, mail, 0)

        assertEquals(card, version.content.preset)
        assertEquals(EntryTimes(created = 1_000L, modified = 900L), version.times)
    }

    @Test
    fun `a version that is not there is refused`() = runTest {
        expectFailure<EntryFailure.EntryNotFound> { opener.open(DB, mail, 3) }
    }

    private fun field(key: String, value: String) = EntryField(key, value, protected = false)

    private fun file(name: String) = NewAttachment(name, byteArrayOf(1))

    private companion object {
        const val DB = "content://db"
    }
}
