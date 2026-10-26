package ru.kino.dev.phone

import kotlinx.coroutines.flow.first
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneSettingsRepository

/**
 * [PhoneDefaults] (pass-docs, Step 77, decisions 6 and 9): the country chosen in the settings while "remember the
 * prefix" is on; else the country of the device - [deviceRegion]; else, a country there are no numbers of, the
 * first calling code - its main country.
 */
internal class SettingsPhoneDefaults(
    private val settings: PhoneSettingsRepository,
    private val phones: PhoneNumbers,
    private val deviceRegion: () -> String,
) : PhoneDefaults {

    override suspend fun region(): String {
        val countries = phones.countries()
        val known = countries.map { it.region }.toSet()
        val chosen = if (settings.rememberPrefix.first()) settings.prefixRegion.first() else null
        return listOfNotNull(chosen, deviceRegion().uppercase()).firstOrNull { it in known }
            ?: countries.first().let { first -> phones.mainRegion(first.callingCode) ?: first.region }
    }
}
