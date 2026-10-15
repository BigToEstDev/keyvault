package ru.kino.dev.core.preset

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import ru.kino.dev.core.expectFailure

class PresetEditorTest {

    private val standardName = "Стандарт"
    private val repository = FakePresetRepository()
    private var ids = 0
    private val editor = PresetEditor(repository, newId = { PresetId("id-${++ids}") })

    @Test
    fun `a preset is made with a new id, the names without the spaces around them`() = runTest {
        val preset = editor.create(" Card ", listOf(listOf(" PIN ")), standardName)

        assertEquals(CustomPreset.of(PresetId("id-1"), "Card", listOf(listOf("PIN"))), preset)
        assertEquals(listOf(preset), repository.presets.first())
    }

    @Test
    fun `a card added and left empty is dropped, the first one stays`() = runTest {
        val preset = editor.create("Card", listOf(emptyList(), emptyList(), listOf("CVC")), standardName)

        assertEquals(listOf(emptyList(), listOf("CVC")), CustomPreset.cards(preset))
    }

    @Test
    fun `a preset with problems is refused with every one of them, pointing at the cards as given`() = runTest {
        val failure = expectFailure<PresetFailure.Invalid> {
            editor.create(" ", listOf(emptyList(), emptyList(), listOf("Password")), standardName)
        }

        assertEquals(
            listOf(
                PresetProblem.Name(PresetNameProblem.EMPTY),
                PresetProblem.FieldName(FieldPosition(2, 0), FieldNameProblem.TAKEN),
            ),
            failure.problems,
        )
        assertEquals(emptyList<EntryPreset>(), repository.presets.first())
    }

    @Test
    fun `no tenth preset of one's own`() = runTest {
        repeat(9) { editor.create("P$it", listOf(emptyList()), standardName) }

        val failure = expectFailure<PresetFailure.Invalid> { editor.create("New", listOf(emptyList()), standardName) }

        assertEquals(listOf(PresetProblem.TooMany), failure.problems)
    }

    @Test
    fun `a preset changed keeps its id and its place`() = runTest {
        val first = editor.create("Card", listOf(emptyList()), standardName)
        editor.create("Wi-Fi", listOf(emptyList()), standardName)

        val changed = editor.update(first.id!!, "Bank card", listOf(listOf("PIN")), standardName)

        assertEquals(first.id, changed.id)
        assertEquals(listOf("Bank card", "Wi-Fi"), repository.presets.first().map { it.name })
    }

    @Test
    fun `a preset deleted elsewhere is not changed`() = runTest {
        expectFailure<PresetFailure.NotFound> {
            editor.update(PresetId("gone"), "Card", listOf(emptyList()), standardName)
        }
    }

    @Test
    fun `a preset is deleted`() = runTest {
        val preset = editor.create("Card", listOf(emptyList()), standardName)

        editor.delete(preset.id!!)

        assertEquals(emptyList<EntryPreset>(), repository.presets.first())
    }

    @Test
    fun `a store that fails is told as it is`() = runTest {
        val notSaved = PresetFailure.NotSaved()
        repository.failure = notSaved

        assertSame(notSaved, expectFailure<PresetFailure.NotSaved> { editor.create("Card", listOf(emptyList()), standardName) })
    }
}
