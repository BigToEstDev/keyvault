package ru.kino.dev.core.phone

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [PhoneNumbers] on a few countries, for the tests of the screens: the numbers of each are of [lengths] digits
 * and grouped by three, the rest of the digits as they are. The real rules are the library's, tested in `core-data`.
 */
class FakePhoneNumbers(
    private val countries: List<PhoneCountry> = listOf(
        PhoneCountry("US", 1),
        PhoneCountry("KZ", 7),
        PhoneCountry("RU", 7),
        PhoneCountry("NL", 31),
    ),
    private val lengths: Map<String, Int> = mapOf("US" to 10, "KZ" to 10, "RU" to 10, "NL" to 9),
) : PhoneNumbers {

    /** The countries [prepare] was asked for, in order. */
    val prepared = mutableListOf<String>()

    override suspend fun prepare(regions: Collection<String>) {
        prepared += regions
    }

    override suspend fun countries(): List<PhoneCountry> = countries

    override fun callingCode(region: String): Int? = countries.firstOrNull { it.region == region }?.callingCode

    override fun mainRegion(callingCode: Int): String? = countries.lastOrNull { it.callingCode == callingCode }?.region

    override fun formatNational(region: String, digits: String): String =
        if (digits.length > (lengths[region] ?: 0)) digits else digits.chunked(3).joinToString(" ")

    override fun isValid(region: String, digits: String): Boolean = digits.length == lengths[region]

    override fun e164(region: String, digits: String): String =
        if (digits.isEmpty()) "" else callingCode(region)?.let { "+$it$digits" } ?: digits

    override fun digits(e164: String, region: String): String {
        val prefix = callingCode(region)?.let { "+$it" }
        return (if (prefix != null && e164.startsWith(prefix)) e164.removePrefix(prefix) else e164).filter(Char::isDigit)
    }
}

/** [PhoneSettingsRepository] in memory. */
class FakePhoneSettingsRepository(remember: Boolean = false, region: String? = null) : PhoneSettingsRepository {

    private val rememberFlow = MutableStateFlow(remember)
    private val regionFlow = MutableStateFlow(region)

    override val rememberPrefix: Flow<Boolean> = rememberFlow
    override val prefixRegion: Flow<String?> = regionFlow

    override suspend fun saveRememberPrefix(on: Boolean) {
        rememberFlow.value = on
    }

    override suspend fun savePrefixRegion(region: String) {
        regionFlow.value = region
    }
}
