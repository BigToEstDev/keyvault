package ru.kino.dev.ffi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class GroupMappingTest {

    @Test
    fun `the nil uuid of the core is no parent and no recycle bin`() {
        val tree = FfiEnvelope.json.decodeFromString(GroupTreeDto.serializer(), TREE).toDomain()

        assertEquals(ROOT, tree.rootId)
        assertNull(tree.recycleBinId)
        assertNull(tree.groups.getValue(ROOT).parentId)
        assertEquals(ROOT, tree.groups.getValue(WORK).parentId)
        assertEquals(listOf(WORK), tree.groups.getValue(ROOT).groupIds)
        assertEquals(listOf(ENTRY), tree.groups.getValue(WORK).entryIds)
    }

    @Test
    fun `a renamed group goes back with every other field as it was read`() {
        val read = FfiEnvelope.json.parseToJsonElement(GROUP).jsonObject
        val group = FfiEnvelope.json.decodeFromJsonElement(GroupDto.serializer(), read)

        val sent = FfiEnvelope.json.encodeToJsonElement(GroupDto.serializer(), group.named("Office")).jsonObject

        assertEquals(JsonObject(read + ("name" to JsonPrimitive("Office"))), sent)
    }

    private companion object {
        const val NIL = "00000000-0000-0000-0000-000000000000"
        const val ROOT = "6f1c3c1e-0000-4000-8000-000000000001"
        const val WORK = "6f1c3c1e-0000-4000-8000-000000000002"
        const val ENTRY = "6f1c3c1e-0000-4000-8000-000000000003"

        val TREE = """
            {
              "root_uuid": "$ROOT",
              "recycle_bin_uuid": "$NIL",
              "deleted_group_uuids": [],
              "groups": {
                "$ROOT": {"uuid": "$ROOT", "parent_group_uuid": "$NIL", "name": "Passwords", "icon_id": 48,
                          "custom_icon_uuid": null, "group_uuids": ["$WORK"], "entry_uuids": []},
                "$WORK": {"uuid": "$WORK", "parent_group_uuid": "$ROOT", "name": "Work", "icon_id": 1,
                          "custom_icon_uuid": null, "group_uuids": [], "entry_uuids": ["$ENTRY"]}
              }
            }
        """.trimIndent()

        // Every field filled with something other than its default, so a field dropped or reset shows
        val GROUP = """
            {
              "uuid": "$WORK",
              "parent_group_uuid": "$ROOT",
              "name": "Work",
              "icon_id": 7,
              "notes": "from KeePassXC",
              "tags": "a;b",
              "is_expanded": true,
              "times": {
                "last_modification_time": "2026-01-02T03:04:05",
                "creation_time": "2025-01-02T03:04:05",
                "last_access_time": "2026-02-02T03:04:05",
                "expires": true,
                "expiry_time": "2027-01-02T03:04:05",
                "location_changed": "2025-06-02T03:04:05",
                "usage_count": 12
              },
              "marked_category": true,
              "default_auto_type_sequence": "{USERNAME}{TAB}{PASSWORD}",
              "enable_auto_type": false,
              "enable_searching": false,
              "custom_icon_uuid": "6f1c3c1e-0000-4000-8000-000000000009",
              "group_uuids": ["6f1c3c1e-0000-4000-8000-000000000004"],
              "entry_uuids": ["$ENTRY"]
            }
        """.trimIndent()
    }
}
