package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.PICKER_ROW_CHECK_TAG
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.feedbackTypes
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class FeedbackScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val submits = mutableListOf<Unit>()
    private val backs = mutableListOf<Unit>()
    private val retries = mutableListOf<Unit>()
    private val dismissals = mutableListOf<Unit>()
    private val selected = mutableListOf<String>()

    private fun ready(
        details: String = "",
        email: String = "ada@example.com",
        selectedTypeId: String? = "bug_report",
        showDetailsError: Boolean = false,
        emailError: String? = null,
        saveState: SaveState = SaveState.Idle,
        dialog: FieldError? = null,
        showThanks: Boolean = false,
    ) = FeedbackUiState(
        load = FeedbackLoad.Ready,
        types = feedbackTypes().types,
        selectedTypeId = selectedTypeId,
        details = details,
        email = email,
        showDetailsError = showDetailsError,
        emailError = emailError,
        saveState = saveState,
        dialog = dialog,
        showThanks = showThanks,
    )

    private fun show(state: FeedbackUiState) {
        rule.setContent {
            ClickMeTheme {
                FeedbackContent(
                    state = state,
                    onSelectType = { selected += it },
                    onDetailsChange = {},
                    onEmailChange = {},
                    onSubmit = { submits += Unit },
                    onRetry = { retries += Unit },
                    onDismissDialog = { dismissals += Unit },
                    onBack = { backs += Unit },
                )
            }
        }
    }

    /** System back while a Compose dialog is showing: the dialog's own back dispatcher handles it. */
    @Suppress("DEPRECATION")
    private fun pressBackOnTheDialog() {
        val dialog = checkNotNull(ShadowDialog.getLatestDialog()) { "no dialog is showing" }
        rule.runOnUiThread { dialog.onBackPressed() }
    }

    @Test
    fun readyFormShowsItsFields() {
        show(ready(details = "The booking screen froze on submit."))

        rule.onNodeWithText("Send Feedback").assertExists()
        rule.onNodeWithText("Feedback Type").assertExists()
        rule.onNodeWithText("Details").assertExists()
        rule.onNodeWithText("Email (Optional)").assertExists()
        rule.onNodeWithText("Submit Feedback").assertExists()
        rule.onNodeWithText("Bug Report").assertExists()
        rule.onNodeWithText("ada@example.com").assertExists()
    }

    @Test
    fun submitCallsSubmit() {
        show(ready(details = "The booking screen froze on submit."))

        rule.onNodeWithTag(FEEDBACK_SUBMIT_TAG).performClick()

        assertEquals(1, submits.size)
    }

    @Test
    fun thanksStateShowsTheDialogAndTheSentButton() {
        show(ready(saveState = SaveState.Saved, showThanks = true))

        rule.onNodeWithText("Thanks for your feedback!").assertExists()
        rule
            .onNodeWithText("We really appreciate you taking the time to help us make ClickMe better.")
            .assertExists()
        rule.onNodeWithText("Feedback Sent!").assertExists()
    }

    @Test
    fun okReturnsToTheHub() {
        show(ready(saveState = SaveState.Saved, showThanks = true))

        rule.onNodeWithText("OK").performClick()

        assertEquals(1, backs.size)
    }

    @Test
    fun systemBackOnThanksReturnsToTheHub() {
        show(ready(saveState = SaveState.Saved, showThanks = true))

        pressBackOnTheDialog()
        rule.waitForIdle()

        assertEquals(1, backs.size)
    }

    @Test
    fun detailsFieldHasItsName() {
        show(ready())

        rule.onNodeWithContentDescription("Details").assertExists()
    }

    // ---- Task 2: types states and the picker sheet -------------------------------------------------------------

    @Test
    fun loadingShowsAFormSkeletonAndNoSubmit() {
        show(FeedbackUiState(load = FeedbackLoad.Loading))

        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(1)
        rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).assertCountEquals(SKELETON_BLOCKS)
        rule.onNodeWithText("Submit Feedback").assertDoesNotExist()
        rule.onNodeWithText("Email (Optional)").assertDoesNotExist()
    }

    @Test
    fun typesFailureShowsRetryAndNoForm() {
        show(FeedbackUiState(load = FeedbackLoad.Failed(FieldError.Text("Server unreachable"))))

        rule.onNodeWithText("Couldn't load categories").assertExists()
        rule.onNodeWithText("Server unreachable").assertExists()
        rule.onNodeWithText("Submit Feedback").assertDoesNotExist()
        rule.onNodeWithText("Details").assertDoesNotExist()
        rule.onNodeWithContentDescription("Details").assertDoesNotExist()

        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries.size)
    }

    @Test
    fun pickerOpensAndChooses() {
        show(ready())

        rule.onNodeWithTag(FEEDBACK_TYPE_FIELD_TAG).performClick()
        rule.waitForIdle()

        // The label above the field and the sheet title share the text "Feedback Type".
        rule.onAllNodesWithText("Feedback Type").assertCountEquals(2)
        listOf("Feature Request", "General Feedback", "Billing Issue", "Content Feedback").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        // "Bug Report" is the field's value and the first row.
        rule.onAllNodesWithText("Bug Report").assertCountEquals(2)
        rule.onAllNodesWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)

        rule.onNodeWithText("Feature Request").performClick()
        rule.waitForIdle()

        assertEquals(listOf("feature_request"), selected)
        rule.onAllNodesWithText("Feedback Type").assertCountEquals(1)
        rule.onNodeWithText("General Feedback").assertDoesNotExist()
    }

    @Test
    fun pickerStateSurvivesRecreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                FeedbackContent(
                    state = ready(),
                    onSelectType = {},
                    onDetailsChange = {},
                    onEmailChange = {},
                    onSubmit = {},
                    onRetry = {},
                    onDismissDialog = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithTag(FEEDBACK_TYPE_FIELD_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("General Feedback").assertExists()

        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithText("General Feedback").assertExists()
    }

    @Test
    fun serverDescriptionIsNotShown() {
        show(ready())
        rule.onNodeWithTag(FEEDBACK_TYPE_FIELD_TAG).performClick()
        rule.waitForIdle()

        feedbackTypes().types.mapNotNull { it.description }.forEach {
            rule.onNodeWithText(it).assertDoesNotExist()
        }
    }

    private companion object {
        /** Three labels, two pills and the Details block. */
        const val SKELETON_BLOCKS = 6
    }
}
