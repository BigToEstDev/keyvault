package ru.kino.dev.core.phone

/**
 * A country a phone number is of: its code - ISO 3166-1 alpha-2, `RU`, `KZ` - and its calling code, 7 for both.
 * One calling code is shared by several countries (+7, +1, +44 …); the code of the country tells them apart
 * (pass-docs, Step 77, decision 3).
 */
data class PhoneCountry(val region: String, val callingCode: Int)

/**
 * Phone numbers as the field of the type "phone" takes them (pass-docs, Step 77): the value is kept in E.164 -
 * `+79001234567` - and shown grouped as its country writes it. A number that is not right for its country is
 * kept all the same - the field is not required - and shown as its digits.
 *
 * The data of a country is read the first time it is asked for, and that blocks: [prepare] reads it ahead, off
 * the main thread. The rest is fast once the countries they name are prepared - a screen prepares what it shows.
 */
interface PhoneNumbers {

    /** Reads the data of the countries of [regions] ahead, off the caller's thread. */
    suspend fun prepare(regions: Collection<String>)

    /** Every country there is a choice of, by calling code, then by code; numbers of no country are not among them. */
    suspend fun countries(): List<PhoneCountry>

    /** The calling code of the country [region]; null for a code there is no country of. */
    fun callingCode(region: String): Int?

    /** The main country of the calling code [callingCode] - `US` of 1, `RU` of 7; null for a code of no country. */
    fun mainRegion(callingCode: Int): String?

    /**
     * The [digits] of a number of [region] after the calling code, grouped as the country writes them while they
     * are typed; digits the country has no grouping for come as they are.
     */
    fun formatNational(region: String, digits: String): String

    /** Whether [digits] after the calling code are a number of [region]. */
    fun isValid(region: String, digits: String): Boolean

    /** The value kept: `+`, the calling code of [region] and the [digits]; none - empty - when there are no digits. */
    fun e164(region: String, digits: String): String

    /** The digits after the calling code of [region] in the value kept [e164]; any other value - its digits. */
    fun digits(e164: String, region: String): String
}
