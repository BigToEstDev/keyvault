package ru.kino.dev.vault.data.generator

import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.generator.GENERATED_PASSWORD
import ru.kino.dev.vault.generator.PasswordGenerator
import kotlin.coroutines.cancellation.CancellationException

/** [PasswordGenerator] over the native core: `generate_password` needs no open database. */
internal class PasswordGeneratorImpl(private val core: NativeCore) : PasswordGenerator {

    override suspend fun generate(): String =
        try {
            core.generatePassword(GENERATED_PASSWORD)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw EntryFailure.Unexpected(e)
        }
}
