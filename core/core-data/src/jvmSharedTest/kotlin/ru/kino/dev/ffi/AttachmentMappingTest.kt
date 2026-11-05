package ru.kino.dev.ffi

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.EntryAttachmentData
import ru.kino.dev.core.NewAttachmentData

/** The attachments of an entry across the boundary (pass-docs, Step 102): the form lists them, new files go in the slot. */
class AttachmentMappingTest {

    private val json = FfiEnvelope.json
    private val form = json.decodeFromString(EntryFormDataDto.serializer(), FORM)

    @Test
    fun `the attachments of an entry come in its order, the hash as the id`() {
        assertEquals(
            listOf(
                EntryAttachmentData(id = "11", name = "a.txt", size = 5),
                EntryAttachmentData(id = "22", name = "b.png", size = 2_000_000),
                EntryAttachmentData(id = "33", name = "c.pdf", size = 0),
            ),
            form.attachmentsToDomain(),
        )
    }

    @Test
    fun `an entry read becomes the domain entry with its attachments`() {
        assertEquals(form.attachmentsToDomain(), form.toEntryData().attachments)
    }

    @Test
    fun `kept attachments go back in the order kept, the rest of each as read`() {
        val kept = form.withAttachmentsKept(listOf("33", "11"))

        assertEquals(listOf("c.pdf", "a.txt"), kept.binaryKeyValues.map { it.key })
        assertEquals(form.binaryKeyValues[2], kept.binaryKeyValues[0])
        assertEquals(form.binaryKeyValues - kept.binaryKeyValues.toSet(), listOf(form.binaryKeyValues[1]))
    }

    @Test
    fun `none kept removes them all`() {
        assertEquals(emptyList(), form.withAttachmentsKept(emptyList()).binaryKeyValues)
    }

    @Test
    fun `an id the entry has not is refused before the core is asked`() {
        try {
            form.withAttachmentsKept(listOf("11", "99"))
            fail("an unknown attachment should not be kept")
        } catch (e: CoreException) {
            assertEquals("DataError", e.kind)
        }
    }

    @Test
    fun `new files go as a list of names and sizes and their bytes one after another`() {
        val slot = attachmentSlot(
            listOf(
                NewAttachmentData("a.txt", byteArrayOf(1, 2, 3)),
                NewAttachmentData("empty", ByteArray(0)),
                NewAttachmentData("b.bin", byteArrayOf(9)),
            ),
        )

        assertEquals(
            listOf(NewAttachmentDto("a.txt", 3), NewAttachmentDto("empty", 0), NewAttachmentDto("b.bin", 1)),
            slot.list,
        )
        assertContentEquals(byteArrayOf(1, 2, 3, 9), slot.bytes)
    }

    @Test
    fun `no new files is no slot at all`() {
        val slot = attachmentSlot(emptyList())

        assertEquals(emptyList(), slot.list)
        assertNull(slot.bytes)
    }

    @Test
    fun `the slot is wiped, the files of the caller are not touched`() {
        val file = byteArrayOf(1, 2, 3)
        val slot = attachmentSlot(listOf(NewAttachmentData("a.txt", file)))

        slot.wipe()

        assertContentEquals(ByteArray(3), slot.bytes)
        assertContentEquals(byteArrayOf(1, 2, 3), file)
    }

    @Test
    fun `the new files are sent in the spelling of the bridge`() {
        val args = EntryFormArgsDto("db", form, listOf(NewAttachmentDto("a.txt", 3)))

        val sent = json.encodeToJsonElement(EntryFormArgsDto.serializer(), args).jsonObject

        assertEquals(
            JsonArray(listOf(JsonObject(mapOf("name" to JsonPrimitive("a.txt"), "size" to JsonPrimitive(3))))),
            sent.getValue("new_attachments"),
        )
    }

    @Test
    fun `a new file shows its name and size, never its bytes`() {
        assertEquals("NewAttachmentData(name=a.txt, size=3)", NewAttachmentData("a.txt", byteArrayOf(7, 7, 7)).toString())
    }

    private companion object {
        val FORM = """
            {
              "uuid": "6f1c3c1e-0000-4000-8000-000000000003",
              "group_uuid": "6f1c3c1e-0000-4000-8000-000000000002",
              "icon_id": 0,
              "last_modification_time": "2026-01-02T03:04:05",
              "creation_time": "2025-01-02T03:04:05",
              "last_access_time": "2026-02-02T03:04:05",
              "expires": false,
              "expiry_time": "2027-01-02T03:04:05",
              "tags": [],
              "binary_key_values": [
                {"key": "a.txt", "value": "", "index_ref": 0, "data_hash": "11", "data_size": 5},
                {"key": "b.png", "value": "", "index_ref": 1, "data_hash": "22", "data_size": 2000000},
                {"key": "c.pdf", "value": "", "index_ref": 2, "data_hash": "33", "data_size": 0}
              ],
              "history_count": 0,
              "fields": [],
              "auto_type": {"enabled": true, "associations": []}
            }
        """.trimIndent()
    }
}
