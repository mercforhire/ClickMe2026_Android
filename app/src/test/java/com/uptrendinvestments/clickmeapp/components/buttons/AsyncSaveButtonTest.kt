package com.uptrendinvestments.clickmeapp.components.buttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Busy-state semantics of [AsyncSaveButton] (Phase 15 POL-04, UI Consideration "loading: Submit buttons"). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AsyncSaveButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Composable
    private fun Button(
        state: SaveState,
        onClick: () -> Unit = {},
        busyDescription: String? = null,
    ) {
        ClickMeTheme {
            if (busyDescription == null) {
                AsyncSaveButton(
                    title = "Save",
                    state = state,
                    onClick = onClick,
                    modifier = Modifier.testTag("btn"),
                )
            } else {
                AsyncSaveButton(
                    title = "Save",
                    state = state,
                    onClick = onClick,
                    modifier = Modifier.testTag("btn"),
                    busyDescription = busyDescription,
                )
            }
        }
    }

    @Test
    fun busyDefaultsToSaving() {
        var clicks = 0
        rule.setContent { Button(SaveState.Saving, onClick = { clicks++ }) }
        rule.onNode(hasStateDescription("Saving")).assertExists()
        rule.onNodeWithTag("btn").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun busyDescriptionOverrides() {
        rule.setContent { Button(SaveState.Saving, busyDescription = "In progress") }
        rule.onNode(hasStateDescription("In progress")).assertExists()
    }

    @Test
    fun busyKeepsSize() {
        var state by mutableStateOf(SaveState.Idle)
        rule.setContent { Button(state) }
        val idle = rule.onNodeWithTag("btn").getUnclippedBoundsInRoot()
        state = SaveState.Saving
        rule.waitForIdle()
        val busy = rule.onNodeWithTag("btn").getUnclippedBoundsInRoot()
        assertEquals(idle.width, busy.width)
        assertEquals(idle.height, busy.height)
    }
}
