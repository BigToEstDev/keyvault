package ru.kino.dev.ui.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.phone.PhoneCountry
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.SearchChoice
import ru.kino.dev.ui.components.SearchChoiceDialog
import java.util.Locale

/**
 * The phone numbers the screens draw a phone field with (pass-docs, Step 77): `app` provides them, as it does
 * [ru.kino.dev.ui.theme.LocalAppDisplay]. Only to draw - a processor prepares the countries a screen shows first,
 * so nothing here reads the data of a country on the main thread.
 */
val LocalPhoneNumbers = staticCompositionLocalOf<PhoneNumbers> { error("the phone numbers are provided by app") }

/** The digits of a number of [region] grouped as the country writes them - for `PhoneField`. */
@Composable
fun rememberPhoneFormat(region: String): (String) -> String {
    val phones = LocalPhoneNumbers.current
    return remember(phones, region) { { digits -> phones.formatNational(region, digits) } }
}

/** The calling code of [region] as it is shown - `+7`. */
@Composable
fun phonePrefix(region: String): String = LocalPhoneNumbers.current.callingCode(region)?.let { "+$it" }.orEmpty()

/** The name of the country [region] in the language of the app. */
@Composable
fun phoneCountryName(region: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return Locale.Builder().setRegion(region).build().getDisplayCountry(locale).ifEmpty { region }
}

/**
 * A number kept - [value], in E.164 - as a screen that reads it shows it: the prefix, then the number grouped; a
 * number that is not of [region] - its digits as they are (pass-docs, Step 77, decision 7).
 */
@Composable
fun phoneShown(value: String, region: String): String {
    if (value.isEmpty()) return value
    val phones = LocalPhoneNumbers.current
    val digits = phones.digits(value, region)
    val prefix = phonePrefix(region)
    return if (value == phones.e164(region, digits)) "$prefix ${phones.formatNational(region, digits)}" else value
}

/** Under a phone field whose number is not of its country - a hint, not an error; none while there is no number. */
@Composable
fun phoneHint(region: String, digits: String): String? {
    if (digits.isEmpty() || LocalPhoneNumbers.current.isValid(region, digits)) return null
    return stringResource(R.string.phone_not_of_country, phoneCountryName(region))
}

/**
 * The choice of the country of a phone (pass-docs, Step 77, decisions 4 and 5): by calling code, each with its
 * prefix before its name; a search over both.
 */
@Composable
fun PhoneCountryDialog(
    countries: List<PhoneCountry>,
    selected: String?,
    onChoose: (String) -> Unit,
    onClose: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val choices = remember(countries, locale) {
        countries.map { country ->
            val name = Locale.Builder().setRegion(country.region).build().getDisplayCountry(locale).ifEmpty { country.region }
            SearchChoice(country.region, name, lead = "+${country.callingCode}")
        }
    }
    SearchChoiceDialog(
        title = stringResource(R.string.phone_country_title),
        choices = choices,
        selected = selected,
        onChoose = onChoose,
        onClose = onClose,
    )
}
