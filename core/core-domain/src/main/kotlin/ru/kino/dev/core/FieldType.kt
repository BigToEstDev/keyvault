package ru.kino.dev.core

/**
 * The types of a field the person names that this app knows (pass-docs, Step 75, item 3). The type is kept in the
 * file - in the entry's CustomData, by the core (Step 76) - as [key]: text is no key at all. A type may carry more
 * after `:` - the country of a phone, `phone:KZ` (Step 77, see [ru.kino.dev.core.phone.PhoneType]).
 *
 * A field keeps the type the file gives it as a string: one this app does not know - written by a newer client -
 * is shown as [TEXT] and goes back as it came, until the person picks another.
 */
enum class FieldType(val key: String?) {
    TEXT(null),
    PHONE("phone"),
    ;

    companion object {
        /** The type of a field of [key] as the file gives it; one this app does not know is shown as text. */
        fun of(key: String?): FieldType {
            val kind = key?.substringBefore(':')
            return entries.firstOrNull { it.key == kind } ?: TEXT
        }
    }
}
