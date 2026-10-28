package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WRITE_REVIEW_SUBMIT_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reviewContext
import com.uptrendinvestments.clickmeapp.testing.submitReviewData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
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
}
