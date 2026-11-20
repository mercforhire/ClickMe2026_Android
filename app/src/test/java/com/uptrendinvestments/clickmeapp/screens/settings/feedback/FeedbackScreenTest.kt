package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PICKER_ROW_CHECK_TAG
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.feedbackTypes
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Composable
    private fun content(state: FeedbackUiState) {
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

    private fun show(state: FeedbackUiState) {
        rule.setContent { ClickMeTheme { content(state) } }
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

    // ---- Task 3: the Details error, the failure dialog and the keyboard ---------------------------------------

    private val tooShort = "Please enter at least 10 characters."
    private val hint = "Describe your experience..."

    @Test
    fun detailsErrorShowsAndClears() {
        var state by mutableStateOf(ready(showDetailsError = true))
        rule.setContent { ClickMeTheme { content(state) } }

        rule.onNodeWithText(tooShort).assertExists()

        state = ready(showDetailsError = false)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(ERROR_ANIMATION_MS)

        rule.onNodeWithText(tooShort).assertDoesNotExist()
    }

    @Test
    fun detailsPlaceholderShowsWhenEmpty() {
        show(ready(details = ""))
        rule.onNodeWithText(hint).assertExists()
    }

    @Test
    fun detailsPlaceholderHidesWithText() {
        show(ready(details = "Something"))
        rule.onNodeWithText(hint).assertDoesNotExist()
    }

    @Test
    fun detailsFieldGrowsAndIsNotCapped() {
        val long = "word ".repeat(LONG_DETAILS_WORDS)
        show(ready(details = long))

        rule.onNodeWithTag(FEEDBACK_DETAILS_TAG).assertTextEquals(long)
        rule.onAllNodesWithText("${long.length}", substring = true).assertCountEquals(0)
        rule.onNodeWithTag(FEEDBACK_SUBMIT_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun detailsFieldHasItsNameWhenFilled() {
        show(ready(details = "Something long enough"))
        rule.onNodeWithContentDescription("Details").assertExists()
    }

    @Test
    fun emailErrorShowsOnItsField() {
        show(ready(emailError = "Invalid email"))

        rule.onNodeWithText("Invalid email").assertExists()
        rule.onNodeWithText("Couldn't send feedback").assertDoesNotExist()
    }

    @Test
    fun failureDialogShowsItsMessageAndKeepsTheForm() {
        show(ready(details = "The booking screen froze.", dialog = FieldError.Text("Server says no")))

        rule.onNodeWithText("Couldn't send feedback").assertExists()
        rule.onNodeWithText("Server says no").assertExists()
        rule.onNodeWithText("The booking screen froze.").assertExists()
        rule.onNodeWithText("ada@example.com").assertExists()
        rule.onNodeWithText("Submit Feedback").assertExists()

        rule.onNodeWithText("OK").performClick()

        assertEquals(1, dismissals.size)
        assertEquals(0, backs.size)
    }

    @Test
    fun tooManyAttemptsAndFallbackCopyRender() {
        show(ready(dialog = FieldError.Res(R.string.feedback_too_many_attempts)))
        rule.onNodeWithText("Too many attempts. Please try again in a moment.").assertExists()
    }

    @Test
    fun fallbackCopyRenders() {
        show(ready(dialog = FieldError.Res(R.string.feedback_send_failed)))
        rule.onNodeWithText("Couldn't send your feedback. Please try again.").assertExists()
    }

    @Test
    fun submittingKeepsFieldsVisibleAndEnabled() {
        show(ready(details = "The booking screen froze.", saveState = SaveState.Saving))

        rule.onNodeWithTag(FEEDBACK_DETAILS_TAG).assertIsEnabled()
        rule.onNodeWithText("The booking screen froze.").assertExists()
        rule.onNodeWithText("ada@example.com").assertExists()
        rule
            .onNodeWithTag(FEEDBACK_SUBMIT_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving"))
        rule.onNodeWithText("Submit Feedback").assertDoesNotExist()
    }

    @Test
    fun labelsPrecedeTheirControls() {
        show(ready(email = ""))

        fun top(node: SemanticsNodeInteraction) = node.getUnclippedBoundsInRoot().top
        val typeLabel = top(rule.onNodeWithText("Feedback Type"))
        val typeField = top(rule.onNodeWithTag(FEEDBACK_TYPE_FIELD_TAG))
        val detailsLabel = top(rule.onNodeWithText("Details"))
        val detailsField = top(rule.onNodeWithTag(FEEDBACK_DETAILS_TAG))
        val emailLabel = top(rule.onNodeWithText("Email (Optional)"))
        val emailField = top(rule.onNodeWithText("you@example.com"))

        assertTrue(typeLabel < typeField)
        assertTrue(typeField < detailsLabel)
        assertTrue(detailsLabel < detailsField)
        assertTrue(detailsField < emailLabel)
        assertTrue(emailLabel < emailField)
    }

    private companion object {
        /** Three labels, two pills and the Details block. */
        const val SKELETON_BLOCKS = 6
        const val LONG_DETAILS_WORDS = 400
        const val ERROR_ANIMATION_MS = 400L
    }
}
