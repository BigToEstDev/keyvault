package ru.kino.dev.ffi

import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.EntrySearchData
import ru.kino.dev.core.EntrySummaryData
import ru.kino.dev.core.HistoryVersionData
import ru.kino.dev.core.TrashEntryData
import ru.kino.dev.core.TrashSearchData
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** The strings of the entry, in its order - all the rest of the form stays in this module. */
internal fun EntryFormDataDto.fieldsToDomain(): List<EntryFieldData> =
    fields.map { EntryFieldData(key = it.key, value = it.value.orEmpty(), protected = it.protected) }

/**
 * The same entry holding [fields] instead of its own. Everything else goes back as read: the core takes
 * the whole form on `insert_entry_from_form_data` and `update_entry_from_form_data`, and a field left out
 * here would be a field reset there.
 *
 * Every field goes as text: the core does not read the data type of a field on the way in - it only
 * reports one, for an otp it can generate codes from.
 */
internal fun EntryFormDataDto.withFields(fields: List<EntryFieldData>) = copy(
    fields = fields.map { KeyValueDataDto(key = it.key, value = it.value, protected = it.protected, dataType = TEXT) },
)

/**
 * A time of the core - a date and time without a zone, in UTC, as `2026-01-02T03:04:05` with or without
 * fractions of a second - in seconds since the epoch. One that cannot be read is null: the entry is still
 * read, only its time is not shown.
 */
internal fun coreTimeToEpochSeconds(time: String): Long? = try {
    LocalDateTime.parse(time).toEpochSecond(ZoneOffset.UTC)
} catch (_: DateTimeParseException) {
    null
}

/**
 * A row of `history_entries_summary` as one old version: its index - the core gives it, its position
 * stands in otherwise - and when it was saved, which the core puts into `secondary_title`.
 */
internal fun EntrySummaryDto.toHistoryVersion(position: Int) = HistoryVersionData(
    index = historyIndex ?: position,
    modifiedAt = secondaryTitle?.let(::coreTimeToEpochSeconds),
)

internal fun EntrySummaryDto.toDomain() = EntrySummaryData(
    id = uuid,
    groupId = parentGroupUuid,
    title = title.orEmpty(),
    // The user name of an entry; only a version from the history has its time there
    userName = secondaryTitle.orEmpty(),
    // Only a version from the history comes without it, and none is asked for here
    createdAt = createdTime ?: 0,
)

internal fun EntrySearchResultDto.toDomain() = EntrySearchData(term = term, entries = entryItems.map { it.toDomain() })

internal fun TrashEntryDto.toDomain() =
    TrashEntryData(entry = entry.toDomain(), previousGroupId = previousGroupUuid, deletedAt = deletedTime)

internal fun TrashSearchResultDto.toDomain() = TrashSearchData(term = term, entries = entryItems.map { it.toDomain() })

/** `entry_summary_data` for the entries directly inside one group. */
internal fun entriesOfGroup(dbKey: String, groupId: String) =
    EntrySummaryArgsDto(dbKey = dbKey, category = EntryCategoryDto(kind = GROUP_CATEGORY, value = groupId))

private const val TEXT = "Text"
private const val GROUP_CATEGORY = "group"
