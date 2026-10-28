package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_COUNTER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_SUBMITTED_BANNER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_SUBMIT_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reviewContext
import com.uptrendinvestments.clickmeapp.testing.submitReviewData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WriteReviewScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private class Calls {
        val ratings = mutableListOf<Int>()
        val comments = mutableListOf<String>()
        var submits = 0
        var back = 0
        var retries = 0
        var dismissed = 0
    }

    private val ready =
        WriteReviewUiState(
            load = WriteReviewLoad.Ready,
            header = ReviewHeader("Ada Lovelace", null, "Algorithms"),
        )

    private fun setContent(
        state: WriteReviewUiState,
        calls: Calls = Calls(),
    ): Calls {
        rule.setContent {
            ClickMeTheme { Content(state, calls) }
        }
        return calls
    }

    @Composable
    private fun Content(
        state: WriteReviewUiState,
        calls: Calls,
    ) {
        WriteReviewContent(
            state = state,
            onBack = { calls.back++ },
            onRetry = { calls.retries++ },
            onRatingSelected = { calls.ratings += it },
            onCommentChanged = { calls.comments += it },
            onSubmit = { calls.submits++ },
            onDismissError = { calls.dismissed++ },
        )
    }

    @Test
    fun readyFormRenders() {
        setContent(ready)
        rule.onNodeWithText("Leave a Review").assertExists()
        rule.onNodeWithText("How was your experience with Ada Lovelace?").assertExists()
        rule.onNodeWithText("Your feedback helps other users and experts.").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithContentDescription("5 stars").assertIsSelected()
        rule.onNodeWithContentDescription("4 stars").assertExists()
        rule.onNodeWithText("Share your experience…").assertExists()
        rule.onNodeWithTag(WRITE_REVIEW_SUBMIT_TAG).assertExists()
        rule.onNodeWithText("Submit review").assertExists()
    }

    @Test
    fun tapsReachCallbacks() {
        val calls = setContent(ready)
        rule.onNodeWithContentDescription("3 stars").performClick()
        rule.onNodeWithTag(WRITE_REVIEW_INPUT_TAG).performTextInput("Great")
        rule.onNodeWithTag(WRITE_REVIEW_SUBMIT_TAG).performClick()
        assertEquals(listOf(3), calls.ratings)
        assertEquals(listOf("Great"), calls.comments)
        assertEquals(1, calls.submits)
    }

    @Test
    fun submittedStatePops() {
        val api = FakeClickMeApi()
        api.reviewContextHandler = { reviewContext() }
        api.submitReviewHandler = { _, _ -> submitReviewData() }
        val viewModel =
            WriteReviewViewModel(
                WriteReviewKey(bookingId = "B1"),
                SavedStateHandle(),
                ReviewsRepository(api),
                ReviewLaunchRequest(),
            )
        val calls = Calls()
        rule.setContent {
            ClickMeTheme {
                WriteReviewScreen(
                    viewModel = viewModel,
                    onBack = { calls.back++ },
                    onSubmitted = { calls.submits++ },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(WRITE_REVIEW_SUBMIT_TAG).performClick()
        rule.waitForIdle()
        assertEquals(1, calls.submits)
        assertEquals(false, viewModel.uiState.value.submitted)
        assertEquals(1, api.submittedReviews.size)
    }

    private fun isRadio() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun starsAreRadioGroup() {
        setContent(ready)
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertExists()
        listOf("1 star", "2 stars", "3 stars", "4 stars", "5 stars").forEach { label ->
            rule.onNodeWithContentDescription(label).assert(isRadio())
        }
        rule.onNodeWithContentDescription("5 stars").assertIsSelected()
        rule.onNodeWithContentDescription("4 stars").assertIsNotSelected()
    }

    @Test
    fun counterFrom800AndCapColor() {
        val state = mutableStateOf(ready.copy(comment = "a".repeat(799)))
        rule.setContent { ClickMeTheme { Content(state.value, Calls()) } }
        rule.onNodeWithTag(WRITE_REVIEW_COUNTER_TAG).assertDoesNotExist()
        state.value = ready.copy(comment = "a".repeat(800))
        rule.waitForIdle()
        rule.onNodeWithTag(WRITE_REVIEW_COUNTER_TAG).assertTextEquals("800/1000")
        assertFalse(state.value.atCap)
        state.value = ready.copy(comment = "a".repeat(1000))
        rule.waitForIdle()
        rule.onNodeWithTag(WRITE_REVIEW_COUNTER_TAG).assertTextEquals("1000/1000")
        assertTrue(state.value.atCap)
    }

    @Test
    fun fieldHasSemanticsLabel() {
        setContent(ready)
        rule.onNodeWithContentDescription("Written review, optional").assertExists()
    }

    @Test
    fun readOnlyState() {
        setContent(ready.copy(readOnly = true, rating = 4, comment = "Helpful"))
        rule.onNodeWithTag(WRITE_REVIEW_SUBMITTED_BANNER_TAG).assertExists()
        rule.onNodeWithText("You've already reviewed this session").assertExists()
        rule.onNodeWithText("Your rating and comment are shown below.").assertExists()
        rule.onNodeWithContentDescription("Rated 4 out of 5").assertHasNoClickAction()
        rule.onNodeWithText("Helpful").assertExists()
        rule.onNodeWithTag(WRITE_REVIEW_SUBMIT_TAG).assertDoesNotExist()
        rule.onNodeWithTag(WRITE_REVIEW_INPUT_TAG).assertDoesNotExist()
    }

    @Test
    fun readOnlyWithoutComment() {
        setContent(ready.copy(readOnly = true, rating = 5, comment = ""))
        rule.onNodeWithText("No written comment.").assertExists()
    }

    @Test
    fun nameFallbackAndNoTopic() {
        setContent(ready.copy(header = ReviewHeader(null, null, null)))
        rule.onNodeWithText("How was your experience with your expert?").assertExists()
        rule.onNodeWithText("Algorithms").assertDoesNotExist()
    }

    @Test
    fun largeFontKeepsStarsAndSubmit() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ClickMeTheme { Content(ready, Calls()) }
            }
        }
        rule.onNodeWithText("How was your experience with Ada Lovelace?").assertExists()
        listOf("1 star", "3 stars", "5 stars").forEach { label ->
            rule.onNodeWithContentDescription(label).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        }
        rule.onNodeWithTag(WRITE_REVIEW_SUBMIT_TAG).assertIsDisplayed()
    }
}
