package ru.kino.dev.vault.data.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.generator.GENERATED_PASSWORD

class PasswordGeneratorImplTest {

    private val core = FakeNativeCore()
    private val generator = PasswordGeneratorImpl(core)

    @Test
    fun `the password comes from the core with the options of the app`() = runTest {
        val password = generator.generate()

        assertEquals(GENERATED_PASSWORD, core.passwordOptions)
        assertEquals(16, password.length)
    }

    @Test
    fun `the generator refusing is unexpected`() = runTest {
        core.failures["generatePassword"] = CoreException("PasswordGenerationFailed", "no")

        expectFailure<EntryFailure.Unexpected> { generator.generate() }
    }
}
