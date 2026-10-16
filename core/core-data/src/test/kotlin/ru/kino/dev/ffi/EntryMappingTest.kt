package ru.kino.dev.ffi

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.HistoryVersionData

class EntryMappingTest {

    private val json = FfiEnvelope.json

    @Test
    fun `the strings of an entry come in its order, a repeated key and protection included`() {
        val form = json.decodeFromString(EntryFormDataDto.serializer(), ENTRY)

        assertEquals(
            listOf(
                EntryFieldData("Title", "Bank", protected = false),
                EntryFieldData("PIN", "1111", protected = true),
                EntryFieldData("Notes", "", protected = false),
                EntryFieldData("PIN", "2222", protected = true),
            ),
            form.fieldsToDomain(),
        )
    }

    @Test
    fun `an entry with new strings goes back with every other field as it was read`() {
        val read = json.parseToJsonElement(ENTRY).jsonObject
        val form = json.decodeFromJsonElement(EntryFormDataDto.serializer(), read)
        val fields = listOf(
            EntryFieldData("Title", "Bank", protected = false),
            EntryFieldData("Password", "secret", protected = true),
        )

        val sent = json.encodeToJsonElement(EntryFormDataDto.serializer(), form.withFields(fields)).jsonObject

        assertEquals(read - "fields", sent - "fields")
        assertEquals(
            listOf("Title" to "Bank", "Password" to "secret"),
            (sent.getValue("fields") as JsonArray).map {
                val field = it.jsonObject
                (field.getValue("key") as JsonPrimitive).content to (field.getValue("value") as JsonPrimitive).content
            },
        )
        assertEquals(JsonPrimitive(true), (sent.getValue("fields") as JsonArray)[1].jsonObject.getValue("protected"))
    }

    @Test
    fun `a summary without a title or a user name has them empty`() {
        val summary = json.decodeFromString(
            EntrySummaryDto.serializer(),
            """{"uuid": "$ENTRY_ID", "parent_group_uuid": "$GROUP_ID", "title": null, "secondary_title": null,
               "icon_id": 0, "custom_icon_uuid": null, "history_index": null, "modified_time": 1, "created_time": 1}""",
        ).toDomain()

        assertEquals(ENTRY_ID, summary.id)
        assertEquals("", summary.title)
        assertEquals("", summary.userName)
    }

    @Test
    fun `a summary carries the group of the entry and when it was made`() {
        val summary = json.decodeFromString(
            EntrySummaryDto.serializer(),
            """{"uuid": "$ENTRY_ID", "parent_group_uuid": "$GROUP_ID", "title": "Bank", "secondary_title": "anton",
               "icon_id": 0, "custom_icon_uuid": null, "history_index": null, "modified_time": 2, "created_time": 1700000000}""",
        ).toDomain()

        assertEquals(GROUP_ID, summary.groupId)
        assertEquals(1_700_000_000L, summary.createdAt)
    }

    @Test
    fun `a search result keeps its term beside the entries`() {
        val result = json.decodeFromString(
            EntrySearchResultDto.serializer(),
            """{"term": "Ban", "entry_items": [{"uuid": "$ENTRY_ID", "parent_group_uuid": "$GROUP_ID",
               "title": "Bank", "secondary_title": null, "icon_id": 0, "custom_icon_uuid": null,
               "history_index": null, "modified_time": 1, "created_time": 1}]}""",
        ).toDomain()

        assertEquals("Ban", result.term)
        assertEquals(listOf(ENTRY_ID), result.entries.map { it.id })
    }

    @Test
    fun `the entries of a group are asked for in the spelling of the bridge`() {
        val sent = json.encodeToJsonElement(EntrySummaryArgsDto.serializer(), entriesOfGroup("db", GROUP_ID))

        assertEquals(
            JsonObject(mapOf("kind" to JsonPrimitive("group"), "value" to JsonPrimitive(GROUP_ID))),
            sent.jsonObject.getValue("category"),
        )
    }

    private companion object {
        const val GROUP_ID = "6f1c3c1e-0000-4000-8000-000000000002"
        const val ENTRY_ID = "6f1c3c1e-0000-4000-8000-000000000003"

        // Every field filled with something other than its default, so a field dropped or reset shows
        val ENTRY = """
            {
              "uuid": "$ENTRY_ID",
              "group_uuid": "$GROUP_ID",
              "icon_id": 12,
              "custom_icon_uuid": "6f1c3c1e-0000-4000-8000-000000000009",
              "last_modification_time": "2026-01-02T03:04:05",
              "creation_time": "2025-01-02T03:04:05",
              "last_access_time": "2026-02-02T03:04:05",
              "expires": true,
              "expiry_time": "2027-01-02T03:04:05",
              "tags": ["a", "b"],
              "binary_key_values": [
                {"key": "notes.txt", "value": "", "index_ref": 0, "data_hash": "123", "data_size": 20}
              ],
              "history_count": 3,
              "fields": [
                {"key": "Title", "value": "Bank", "protected": false, "data_type": "Text",
                 "password_score": null, "current_opt_token": null},
                {"key": "PIN", "value": "1111", "protected": true, "data_type": "Text",
                 "password_score": null, "current_opt_token": null},
                {"key": "Notes", "value": null, "protected": false, "data_type": "Text",
                 "password_score": null, "current_opt_token": null},
                {"key": "PIN", "value": "2222", "protected": true, "data_type": "Text",
                 "password_score": null, "current_opt_token": null}
              ],
              "auto_type": {"enabled": false, "data_transfer_obfuscation": 1,
                            "default_sequence": "{USERNAME}{TAB}{PASSWORD}", "associations": []},
              "parsed_fields": {"USERNAME": "x"}
            }
        """.trimIndent()
    }

    @Test
    fun `a time of the core is read as UTC, with or without fractions of a second`() {
        assertEquals(1_767_323_045L, coreTimeToEpochSeconds("2026-01-02T03:04:05"))
        assertEquals(1_767_323_045L, coreTimeToEpochSeconds("2026-01-02T03:04:05.123456"))
    }

    @Test
    fun `a time that cannot be read is null, not a failure`() {
        assertEquals(null, coreTimeToEpochSeconds("not a time"))
        assertEquals(null, coreTimeToEpochSeconds(""))
    }

    @Test
    fun `a row of the history is a version with its index and the time it was saved`() {
        val row = EntrySummaryDto(
            uuid = "e",
            parentGroupUuid = "g",
            title = "Mail",
            secondaryTitle = "2026-01-02T03:04:05",
            iconId = 0,
            historyIndex = 3,
        )

        assertEquals(HistoryVersionData(index = 3, modifiedAt = 1_767_323_045L), row.toHistoryVersion(position = 0))
    }

    @Test
    fun `a row without an index takes its position, one without a time has none`() {
        val row = EntrySummaryDto(uuid = "e", parentGroupUuid = "g", iconId = 0)

        assertEquals(HistoryVersionData(index = 1, modifiedAt = null), row.toHistoryVersion(position = 1))
    }
}
