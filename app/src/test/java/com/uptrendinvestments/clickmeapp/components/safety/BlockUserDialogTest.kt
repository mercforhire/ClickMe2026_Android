package com.uptrendinvestments.clickmeapp.components.safety

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlockUserDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    private fun show(
        name: String = "Ada",
        inFlight: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                BlockUserDialog(
                    name = name,
                    inFlight = inFlight,
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    @Test
    fun showsTheNameAndTheMessage() {
        show()

        rule.onNodeWithText("Block Ada?").assertExists()
        rule.onNodeWithText("They won't be able to message you.").assertExists()
    }

    @Test
    fun aBlankNameFallsBackToGenericTitle() {
        show(name = " ")

        rule.onNodeWithText("Block this user?").assertExists()
    }

    @Test
    fun cancelDismissesAndBlockConfirms() {
        show()

        rule.onNodeWithTag(BLOCK_DIALOG_CANCEL_TAG).performClick()
        assertEquals(1, dismissed)
        rule.onNodeWithTag(BLOCK_DIALOG_CONFIRM_TAG).performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun inFlightShowsSpinnerAndIgnoresEveryTap() {
        show(inFlight = true)

        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Blocking")).assertExists()
        rule.onNodeWithTag(BLOCK_DIALOG_CANCEL_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(BLOCK_DIALOG_CANCEL_TAG).performClick()
        rule.onNodeWithTag(BLOCK_DIALOG_CONFIRM_TAG).performClick()

        assertEquals(0, dismissed)
        assertEquals(0, confirmed)
    }
}
