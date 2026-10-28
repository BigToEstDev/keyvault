package ru.kino.dev.vault.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.TITLE
import ru.kino.dev.vault.group.GroupId

class EntryOpenerTest {

    private val card = CustomPreset.of(PresetId("card"), "Card", listOf(listOf("PIN")))
    private val entries = FakeEntryRepository()
    private val presets = FakePresetRepository(card)
    private val opener = EntryOpener(entries, presets)

    @Test
    fun `an entry without a link opens with the standard preset, in its group`() = runTest {
        val id = entries.add("mail", GroupId("work"), listOf(field(TITLE, "Mail"), field("PIN", "1")))

        val opened = opener.open(DB, id)

        assertEquals(GroupId("work"), opened.groupId)
        assertEquals(StandardPreset.preset, opened.content.preset)
        assertEquals(listOf("PIN"), opened.content.ownFields.map { it.key })
    }

    @Test
    fun `an entry opens with when it was made and last changed`() = runTest {
        val id = entries.add("mail", GroupId("work"), listOf(field(TITLE, "Mail")), createdAt = 1_000L)
        entries.stored[id] = entries.stored.getValue(id).copy(modifiedAt = 2_000L)

        val opened = opener.open(DB, id)

        assertEquals(EntryTimes(created = 1_000L, modified = 2_000L), opened.times)
    }

    @Test
    fun `an entry linked to a preset of one's own opens with it`() = runTest {
        val id = entries.add("visa", GroupId("work"), listOf(field(TITLE, "Visa"), field("PIN", "1"), field(PresetLink.KEY, "card")))

        val content = opener.open(DB, id).content

        assertEquals(card, content.preset)
        assertEquals("1", content.value("PIN"))
        assertEquals(emptyList<EntryField>(), content.ownFields)
    }

    @Test
    fun `a link to a preset this app does not have opens with the standard one, its fields as own`() = runTest {
        val id = entries.add("visa", GroupId("work"), listOf(field(TITLE, "Visa"), field("PIN", "1"), field(PresetLink.KEY, "gone")))

        val content = opener.open(DB, id).content

        assertEquals(StandardPreset.preset, content.preset)
        assertEquals(listOf(field("PIN", "1")), content.ownFields)
    }

    @Test
    fun `an entry opened after its preset is deleted is of the standard one`() = runTest {
        val id = entries.add("visa", GroupId("work"), listOf(field(PASSWORD, "x"), field(PresetLink.KEY, "card")))
        presets.delete(PresetId("card"))

        assertEquals(StandardPreset.preset, opener.open(DB, id).content.preset)
    }

    private fun field(key: String, value: String) = EntryField(key, value, protected = false)

    private companion object {
        const val DB = "db"
    }
}
