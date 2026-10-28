package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewSubmitted
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reviewContext
import com.uptrendinvestments.clickmeapp.testing.submitReviewData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WriteReviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = ReviewLaunchRequest()

    init {
        api.reviewContextHandler = { reviewContext() }
        api.submitReviewHandler = { _, _ -> submitReviewData() }
    }

    private fun vm(
        key: WriteReviewKey = WriteReviewKey(bookingId = "B1"),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = WriteReviewViewModel(key, handle, ReviewsRepository(api), launchRequest)

    @Test
    fun contextLoadsIntoReadyWithDefaults() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler = { reviewContext(name = "Ada Lovelace", topic = "Algorithms") }
            val vm = vm()
            runCurrent()
            val state = vm.uiState.value
            assertEquals(WriteReviewLoad.Ready, state.load)
            assertEquals("Ada Lovelace", state.header.expertName)
            assertEquals("Algorithms", state.header.sessionTopic)
            assertEquals(5, state.rating)
            assertEquals("", state.comment)
            assertFalse(state.readOnly)
            assertEquals(listOf("B1"), api.reviewContextRequests)
            // Loading never posts a review (prohibition: only a Submit tap does).
            assertTrue(api.submittedReviews.isEmpty())
        }

    @Test
    fun submitPostsDefaultAndHandsOff() =
        runTest(main.testDispatcher) {
            val vm = vm(WriteReviewKey(bookingId = "B1", opener = ReviewOpener.BookingSummary))
            runCurrent()
            vm.submit()
            runCurrent()
            assertEquals(listOf("B1" to SubmitReviewRequest(5, null)), api.submittedReviews)
            assertTrue(vm.uiState.value.submitted)
            assertEquals(ReviewSubmitted("B1"), launchRequest.summaryReviewed.pending.value)
            assertNull(launchRequest.bookingsReviewed.pending.value)
            assertEquals(setOf("B1"), launchRequest.reviewedBookingIds.value)
            vm.onSubmittedHandled()
            assertFalse(vm.uiState.value.submitted)
        }
}
