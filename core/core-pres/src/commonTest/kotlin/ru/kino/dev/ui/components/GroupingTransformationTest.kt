package ru.kino.dev.ui.components

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupingTransformationTest {

    // As a country writes ten digits: 900 123-45-67
    private val grouping = GroupingTransformation { digits ->
        buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3) append(' ')
                if (i == 6 || i == 8) append('-')
                append(c)
            }
        }
    }

    @Test
    fun `the digits are drawn grouped`() {
        assertEquals("900 123-45-67", grouping.filter(AnnotatedString("9001234567")).text.text)
    }

    @Test
    fun `the cursor after a digit stands right after it, the spaces and dashes skipped`() {
        val mapping = grouping.filter(AnnotatedString("9001234567")).offsetMapping

        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(13, mapping.originalToTransformed(10))
    }

    @Test
    fun `a place in what is drawn counts the digits before it`() {
        val mapping = grouping.filter(AnnotatedString("9001234567")).offsetMapping

        assertEquals(3, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(10, mapping.transformedToOriginal(13))
    }

    @Test
    fun `a grouping that loses a digit is not drawn`() {
        val lossy = GroupingTransformation { it.drop(1) }

        assertEquals("123", lossy.filter(AnnotatedString("123")).text.text)
    }
}
