package ru.kino.dev.phone

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneSettingsRepository
import java.util.Locale
import javax.inject.Singleton

/**
 * Phone numbers: shared by `vault` - the field of the type "phone" - and `settings` - the prefix by default - so
 * they live here and not in a feature (pass-docs, Step 77).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface PhoneModule {

    @Binds
    fun bindPhoneNumbers(impl: LibPhoneNumbers): PhoneNumbers

    companion object {

        @Provides
        @Singleton
        fun providePhoneSettings(@ApplicationContext context: Context): PhoneSettingsRepository =
            DataStorePhoneSettingsRepository(context.phoneStore)

        // The country of the device, not of the language chosen in the app: the app changes only the language
        @Provides
        fun providePhoneDefaults(settings: PhoneSettingsRepository, phones: PhoneNumbers): PhoneDefaults =
            SettingsPhoneDefaults(settings, phones) { Locale.getDefault().country }
    }
}
