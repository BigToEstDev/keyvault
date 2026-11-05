package ru.kino.dev.vault.generator

/**
 * [PasswordGenerator] for tests: [passwords] one after another - `generated-1`, `generated-2`, ... past them;
 * [failure], when set, is thrown instead.
 */
class FakePasswordGenerator(private val passwords: List<String> = emptyList()) : PasswordGenerator {

    var failure: Exception? = null

    /** How many were asked for. */
    var calls = 0
        private set

    override suspend fun generate(): String {
        calls++
        failure?.let { throw it }
        return passwords.getOrNull(calls - 1) ?: "generated-$calls"
    }
}
