package ru.kino.dev.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.ui.components.SecretCharKind.Digit
import ru.kino.dev.ui.components.SecretCharKind.Letter
import ru.kino.dev.ui.components.SecretCharKind.Symbol

class SecretRunsTest {

    @Test
    fun `an empty secret has no stretches`() {
        assertEquals(emptyList<SecretRun>(), secretRuns(""))
    }

    @Test
    fun `letters, digits and symbols each go in a stretch of their own`() {
        assertEquals(
            listOf(SecretRun(0, 3, Letter), SecretRun(3, 5, Digit), SecretRun(5, 7, Symbol), SecretRun(7, 8, Letter)),
            secretRuns("aBc12!#z"),
        )
    }

    @Test
    fun `cyrillic is a letter, a space is a symbol`() {
        assertEquals(
            listOf(SecretRun(0, 3, Letter), SecretRun(3, 4, Symbol), SecretRun(4, 5, Digit)),
            secretRuns("Ёжи 7"),
        )
    }

    @Test
    fun `the stretches cover the whole secret`() {
        val text = "p@ss w0rd-№1"

        val runs = secretRuns(text)

        assertEquals(0, runs.first().start)
        assertEquals(text.length, runs.last().end)
        runs.zipWithNext { a, b -> assertEquals(a.end, b.start) }
    }
}
