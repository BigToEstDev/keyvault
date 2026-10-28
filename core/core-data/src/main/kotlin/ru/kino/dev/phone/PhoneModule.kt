package ru.kino.dev.phone

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneSettingsRepository
import java.util.Locale

/**
 * Phone numbers: shared by `vault` - the field of the type "phone" - and `settings` - the prefix by default - so
 * they live here and not in a feature (pass-docs, Step 77).
 */
internal val phoneModule = module {
    single<PhoneNumbers> { LibPhoneNumbers() }

    single<PhoneSettingsRepository> { DataStorePhoneSettingsRepository(androidContext().phoneStore) }

    // The country of the device, not of the language chosen in the app: the app changes only the language
    factory<PhoneDefaults> { SettingsPhoneDefaults(get(), get()) { Locale.getDefault().country } }
}
