package ru.kino.dev.settings.data.biometric

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * [BiometricPasswordStore] on a preferences data store of its own, apart from the remembered database.
 *
 * What is written is ciphertext of a key that never leaves the Keystore - useless without the user's
 * biometrics on this phone.
 */
internal class DataStoreBiometricPasswordStore(
    private val store: DataStore<Preferences>,
) : BiometricPasswordStore {

    override suspend fun read(): StoredPassword? =
        store.data.first()[ENTRY]
            // A record that cannot be read is none: biometrics are offered to be turned on again
            ?.let { runCatching { json.decodeFromString(StoredDto.serializer(), it).toStored() }.getOrNull() }

    override suspend fun write(password: StoredPassword) {
        store.edit { it[ENTRY] = json.encodeToString(StoredDto.serializer(), password.toDto()) }
    }

    override suspend fun clear() {
        store.edit { it.remove(ENTRY) }
    }

    @Serializable
    private data class StoredDto(
        @SerialName("db_uri") val dbUri: String,
        val iv: String,
        val ciphertext: String,
    )

    private fun StoredPassword.toDto() =
        StoredDto(dbUri, base64.encodeToString(iv), base64.encodeToString(ciphertext))

    private fun StoredDto.toStored() =
        StoredPassword(dbUri, Base64.getDecoder().decode(iv), Base64.getDecoder().decode(ciphertext))

    private companion object {
        val ENTRY: Preferences.Key<String> = stringPreferencesKey("password")
        val json = Json { ignoreUnknownKeys = true }
        val base64: Base64.Encoder = Base64.getEncoder()
    }
}
