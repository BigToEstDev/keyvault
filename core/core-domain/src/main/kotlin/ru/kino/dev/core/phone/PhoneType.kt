package ru.kino.dev.core.phone

import ru.kino.dev.core.FieldType

/**
 * The type of a phone field as the file keeps it (pass-docs, Step 77, decision 3): `phone:KZ` - the type and the
 * code of the country after it; `phone` alone - no country yet: the field of a preset, which a new entry gives the
 * country by default.
 */
object PhoneType {

    private const val SEPARATOR = ':'

    /** The country of the phone field of the type [raw]; null when there is none - or it is no phone field. */
    fun region(raw: String?): String? {
        if (FieldType.of(raw) != FieldType.PHONE) return null
        return raw?.substringAfter(SEPARATOR, missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }
    }

    /** The type of a phone field of the country [region]. */
    fun of(region: String): String = "${FieldType.PHONE.key}$SEPARATOR$region"
}
