package ru.kino.dev.ui.components

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The error bubble goes with its field: scrolled out of sight - even a little - the bubble is not shown, the "!" is;
 * back in sight - the bubble is again (pass-docs, Step 72, item 7).
 */
@RunWith(RobolectricTestRunner::class)
class FieldErrorScrollTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scroll: ScrollState

    @Test
    fun `a field in sight shows its bubble`() {
        show()

        compose.onNodeWithText(ERROR).assertExists()
    }

    @Test
    fun `a field scrolled a little out of sight hides its bubble, the icon stays`() {
        show()

        // Into the "!": its top half under the edge
        scrollTo(SPACER_PX + 50)

        compose.onNodeWithText(ERROR).assertDoesNotExist()
        compose.onNodeWithContentDescription(ICON, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a field back in sight shows its bubble again`() {
        show()

        scrollTo(SPACER_PX + 400)
        scrollTo(0)

        compose.onNodeWithText(ERROR).assertExists()
    }

    private fun show() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            scroll = rememberScrollState()
            // A short window onto a long form: the field is right under its top
            Column(
                modifier = Modifier
                    .height(300.dp)
                    .verticalScroll(scroll),
            ) {
                Spacer(Modifier.height(SPACER_DP.dp))
                TextField(value = "", onValueChange = {}, label = "PIN", error = ERROR, modifier = Modifier.padding(8.dp))
                Spacer(Modifier.height(1000.dp))
            }
        }
        compose.waitForIdle()
    }

    private fun scrollTo(px: Int) {
        compose.runOnIdle { runBlocking { scroll.scrollTo(px) } }
        compose.waitForIdle()
    }

    private companion object {
        const val ERROR = "Required"
        const val ICON = "Show error"
        const val SPACER_DP = 10

        // Robolectric draws at 160 dpi: a dp is a pixel
        const val SPACER_PX = SPACER_DP
    }
}
