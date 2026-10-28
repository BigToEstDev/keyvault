package ru.kino.dev.phone

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kino.dev.core.phone.PhoneCountry
import ru.kino.dev.core.phone.PhoneNumbers

/**
 * [PhoneNumbers] on Google's libphonenumber (pass-docs, Step 77, decision 2) - offline, it asks nothing of the net.
 *
 * The library reads the data of a country from its resources the first time the country is asked for, and on
 * Android that is slow: [prepare] and [countries] do it on the io dispatcher, so the screens never wait for it on
 * the main thread.
 */
internal class LibPhoneNumbers : PhoneNumbers {

    // Fixed, not a parameter: see NativeCoreFfi
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    // Made on first use: it reads the list of countries
    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    override suspend fun prepare(regions: Collection<String>) = withContext(dispatcher) {
        // Asking for the calling code reads the data of the country
        regions.forEach { util.getCountryCodeForRegion(it) }
    }

    override suspend fun countries(): List<PhoneCountry> = withContext(dispatcher) {
        util.supportedRegions
            .map { PhoneCountry(it, util.getCountryCodeForRegion(it)) }
            .sortedWith(compareBy({ it.callingCode }, { it.region }))
    }

    override fun callingCode(region: String): Int? = util.getCountryCodeForRegion(region).takeIf { it != 0 }

    override fun mainRegion(callingCode: Int): String? =
        util.getRegionCodeForCountryCode(callingCode).takeIf { it != UNKNOWN_REGION && it != NON_GEO_REGION }

    override fun formatNational(region: String, digits: String): String {
        val code = callingCode(region) ?: return digits
        if (digits.isEmpty()) return ""
        // Typed in full, the calling code first: the national grouping of some countries adds a trunk prefix
        val formatter = util.getAsYouTypeFormatter(region)
        var typed = ""
        "+$code$digits".forEach { typed = formatter.inputDigit(it) }
        val national = typed.removePrefix("+$code").trimStart()
        // A grouping must keep every digit, in order; anything else - the digits as they are
        return if (national.filter(Char::isDigit) == digits) national else digits
    }

    override fun isValid(region: String, digits: String): Boolean {
        val code = callingCode(region) ?: return false
        return try {
            util.isValidNumberForRegion(util.parse("+$code$digits", region), region)
        } catch (_: NumberParseException) {
            false
        }
    }

    override fun e164(region: String, digits: String): String {
        if (digits.isEmpty()) return ""
        val code = callingCode(region) ?: return digits
        return "+$code$digits"
    }

    override fun digits(e164: String, region: String): String {
        val prefix = callingCode(region)?.let { "+$it" }
        val rest = if (prefix != null && e164.startsWith(prefix)) e164.removePrefix(prefix) else e164
        return rest.filter(Char::isDigit)
    }

    private companion object {
        // What the library answers for a calling code of no country, and for one of numbers of no country
        const val UNKNOWN_REGION = "ZZ"
        const val NON_GEO_REGION = "001"
    }
}
