package ru.kino.dev.phone

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneSettingsRepository
import ru.kino.dev.storage.AppDataDir
import ru.kino.dev.storage.preferencesStore
import java.util.Locale

private const val PHONE_STORE = "phone_settings"

/**
 * Phone numbers: shared by `vault` - the field of the type "phone" - and `settings` - the prefix by default - so
 * they live here and not in a feature (pass-docs, Step 77). On the jvm of both platforms: libphonenumber is Java.
 */
internal val phoneModule = module {
    single<PhoneNumbers> { LibPhoneNumbers() }

    // One store per file for the whole process (pass-docs, Step 86)
    single(named(PHONE_STORE)) { get<AppDataDir>().preferencesStore(PHONE_STORE) }

    single<PhoneSettingsRepository> { DataStorePhoneSettingsRepository(get(named(PHONE_STORE))) }

    // The country of the device, not of the language chosen in the app: the app changes only the language
    factory<PhoneDefaults> { SettingsPhoneDefaults(get(), get()) { Locale.getDefault().country } }
}
