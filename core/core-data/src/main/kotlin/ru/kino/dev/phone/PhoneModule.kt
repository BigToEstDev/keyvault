package ru.kino.dev.phone

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kino.dev.core.phone.PhoneNumbers

/**
 * Phone numbers: shared by `vault` - the field of the type "phone" - and `settings` - the prefix by default - so
 * they live here and not in a feature (pass-docs, Step 77).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface PhoneModule {

    @Binds
    fun bindPhoneNumbers(impl: LibPhoneNumbers): PhoneNumbers
}
