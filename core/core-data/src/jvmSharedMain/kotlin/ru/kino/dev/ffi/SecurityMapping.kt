package ru.kino.dev.ffi

import ru.kino.dev.core.Cipher
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.KdfVariant

/** The security of the database out of its settings; the core spells the enums as the names here. */
internal fun DbSettingsDto.securityToDomain(): DatabaseSecurity = DatabaseSecurity(
    encryption = Encryption(
        cipher = Cipher.valueOf(cipherId),
        kdf = KdfVariant.valueOf(kdf.algorithm),
        iterations = kdf.iterations,
    ),
    memoryBytes = kdf.memory,
    parallelism = kdf.parallelism,
    credentials = CredentialsUsed(password = passwordUsed, keyFile = keyFileUsed),
    keyFileName = keyFileName,
)

/** The settings with [encryption]; memory, lanes, the Argon2 version and the rest go back as the core gave them. */
internal fun DbSettingsDto.withEncryption(encryption: Encryption): DbSettingsDto = copy(
    cipherId = encryption.cipher.name,
    kdf = kdf.copy(algorithm = encryption.kdf.name, iterations = encryption.iterations),
)

/**
 * The settings with [change], the four flags put together here and nowhere else (pass-docs, Step 67).
 *
 * The core takes `*_used` / `*_changed` from its caller and checks only some combinations, so they are derived
 * from one change and the credentials the settings were read with: what is not changed keeps its use as the
 * database has it, and the password goes only with its own change. A new key file goes beside the settings, as
 * content - the caller adds it for [CredentialsChange.SetKeyFile].
 */
internal fun DbSettingsDto.withCredentials(change: CredentialsChange): DbSettingsDto {
    val unchanged = copy(password = null, passwordChanged = false, keyFileChanged = false)
    return when (change) {
        is CredentialsChange.SetPassword ->
            unchanged.copy(password = change.password, passwordUsed = true, passwordChanged = true)
        is CredentialsChange.SetKeyFile -> unchanged.copy(keyFileUsed = true, keyFileChanged = true)
        CredentialsChange.RemoveKeyFile -> unchanged.copy(keyFileUsed = false, keyFileChanged = true)
    }
}
