package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewSubmitted
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reviewContext
import com.uptrendinvestments.clickmeapp.testing.submitReviewData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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

    @Test
    fun ratingChangesWithinRange() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onRatingSelected(3)
            assertEquals(3, vm.uiState.value.rating)
            vm.onRatingSelected(0)
            vm.onRatingSelected(6)
            assertEquals(3, vm.uiState.value.rating)
        }

    @Test
    fun commentIsCappedAt1000() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onCommentChanged("a".repeat(1200))
            assertEquals(1000, vm.uiState.value.comment.length)
            assertTrue(vm.uiState.value.atCap)
            vm.onCommentChanged("a".repeat(799))
            assertFalse(vm.uiState.value.showCounter)
            assertFalse(vm.uiState.value.atCap)
            vm.onCommentChanged("a".repeat(800))
            assertTrue(vm.uiState.value.showCounter)
        }

    @Test
    fun submitSendsTrimmedComment() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onCommentChanged("  Great session  ")
            vm.submit()
            runCurrent()
            assertEquals(listOf("B1" to SubmitReviewRequest(5, "Great session")), api.submittedReviews)
        }

    @Test
    fun blankCommentIsOmitted() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onCommentChanged("   ")
            vm.submit()
            runCurrent()
            assertEquals(listOf("B1" to SubmitReviewRequest(5, null)), api.submittedReviews)
        }

    @Test
    fun alreadySubmittedIsReadOnly() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler =
                { reviewContext(hasSubmitted = true, prefillRating = 4, prefillComment = "Helpful") }
            val vm = vm()
            runCurrent()
            val state = vm.uiState.value
            assertTrue(state.readOnly)
            assertEquals(4, state.rating)
            assertEquals("Helpful", state.comment)
            vm.onRatingSelected(2)
            vm.onCommentChanged("x")
            vm.submit()
            runCurrent()
            assertEquals(4, vm.uiState.value.rating)
            assertEquals("Helpful", vm.uiState.value.comment)
            assertTrue(api.submittedReviews.isEmpty())
            assertFalse(vm.uiState.value.submitted)
        }

    @Test
    fun prefillWithoutCommentGivesEmptyComment() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler = { reviewContext(hasSubmitted = true, prefillRating = 3) }
            val vm = vm()
            runCurrent()
            assertEquals("", vm.uiState.value.comment)
            assertEquals(3, vm.uiState.value.rating)
        }

    @Test
    fun readOnlyIgnoresSavedState() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler =
                { reviewContext(hasSubmitted = true, prefillRating = 4, prefillComment = "Done") }
            val vm = vm(handle = SavedStateHandle(mapOf("rating" to 1, "comment" to "stale")))
            runCurrent()
            assertEquals(4, vm.uiState.value.rating)
            assertEquals("Done", vm.uiState.value.comment)
        }

    @Test
    fun inFlightIgnoresInput() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.submitReviewHandler = { _, _ ->
                gate.await()
                submitReviewData()
            }
            val vm = vm()
            runCurrent()
            vm.submit()
            runCurrent()
            assertTrue(vm.uiState.value.submitting)
            vm.submit()
            vm.onRatingSelected(1)
            vm.onCommentChanged("x")
            runCurrent()
            assertEquals(5, vm.uiState.value.rating)
            assertEquals("", vm.uiState.value.comment)
            gate.complete(Unit)
            runCurrent()
            assertEquals(1, api.submittedReviews.size)
            assertTrue(vm.uiState.value.submitted)
        }

    @Test
    fun savedStateRestoresForm() =
        runTest(main.testDispatcher) {
            val vm = vm(handle = SavedStateHandle(mapOf("rating" to 2, "comment" to "Later")))
            runCurrent()
            assertEquals(WriteReviewLoad.Ready, vm.uiState.value.load)
            assertEquals(2, vm.uiState.value.rating)
            assertEquals("Later", vm.uiState.value.comment)
        }

    @Test
    fun editsAreMirroredToSavedState() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = vm(handle = handle)
            runCurrent()
            vm.onRatingSelected(4)
            vm.onCommentChanged("Nice")
            assertEquals(4, handle.get<Int>("rating"))
            assertEquals("Nice", handle.get<String>("comment"))
        }

    @Test
    fun headerFallsBackToSeed() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler = { reviewContext(name = null, avatarUrl = null) }
            val seeded =
                vm(WriteReviewKey(bookingId = "B1", expertName = "Seed Name", expertAvatarUrl = "https://img/s"))
            runCurrent()
            assertEquals("Seed Name", seeded.uiState.value.header.expertName)
            assertEquals("https://img/s", seeded.uiState.value.header.expertAvatarUrl)
            val bare = vm()
            runCurrent()
            assertNull(bare.uiState.value.header.expertName)
            assertNull(bare.uiState.value.header.expertAvatarUrl)
        }

    @Test
    fun noRequestWithoutSubmit() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.onRatingSelected(2)
            vm.onCommentChanged("draft")
            runCurrent()
            assertTrue(api.submittedReviews.isEmpty())
            assertFalse(vm.uiState.value.submitted)
            assertTrue(launchRequest.reviewedBookingIds.value.isEmpty())
        }

    @Test
    fun contextFailureThenRetry() =
        runTest(main.testDispatcher) {
            api.reviewContextHandler = {
                throw api.httpError(
                    404,
                    """{"status":"error","code":"NOT_FOUND","message":"Booking not found or not completed."}""",
                )
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                WriteReviewLoad.Failed(FieldError.Text("Booking not found or not completed.")),
                vm.uiState.value.load,
            )
            api.reviewContextHandler = { throw IOException("offline") }
            vm.retry()
            runCurrent()
            assertEquals(WriteReviewLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)
            api.reviewContextHandler = { reviewContext() }
            vm.retry()
            runCurrent()
            assertEquals(WriteReviewLoad.Ready, vm.uiState.value.load)
        }

    @Test
    fun submitServerErrorKeepsForm() =
        runTest(main.testDispatcher) {
            api.submitReviewHandler = { _, _ ->
                throw api.httpError(
                    422,
                    """{"status":"error","code":"VALIDATION_ERROR","message":"Validation failed"}""",
                )
            }
            val vm = vm()
            runCurrent()
            vm.onRatingSelected(2)
            vm.onCommentChanged("Hello")
            vm.submit()
            runCurrent()
            val state = vm.uiState.value
            assertEquals(FieldError.Text("Validation failed"), state.submitError)
            assertFalse(state.submitting)
            assertEquals(2, state.rating)
            assertEquals("Hello", state.comment)
            assertFalse(state.submitted)
            assertNull(launchRequest.summaryReviewed.pending.value)
            assertTrue(launchRequest.reviewedBookingIds.value.isEmpty())
        }

    @Test
    fun submitNetworkError() =
        runTest(main.testDispatcher) {
            api.submitReviewHandler = { _, _ -> throw IOException("offline") }
            val vm = vm()
            runCurrent()
            vm.submit()
            runCurrent()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.submitError)
        }

    @Test
    fun submitErrorWithoutMessageUsesGenericCopy() =
        runTest(main.testDispatcher) {
            api.submitReviewHandler = { _, _ -> throw api.httpError(500, "{}") }
            val vm = vm()
            runCurrent()
            vm.submit()
            runCurrent()
            assertEquals(FieldError.Res(R.string.write_review_error_generic), vm.uiState.value.submitError)
            api.submitReviewHandler = { _, _ -> throw IllegalStateException("bad payload") }
            vm.dismissSubmitError()
            vm.submit()
            runCurrent()
            assertEquals(FieldError.Res(R.string.write_review_error_generic), vm.uiState.value.submitError)
        }

    @Test
    fun suspendedShowsNoLocalDialog() =
        runTest(main.testDispatcher) {
            api.submitReviewHandler = { _, _ ->
                throw api.httpError(
                    403,
                    """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Account suspended."}""",
                )
            }
            val vm = vm()
            runCurrent()
            vm.submit()
            runCurrent()
            assertNull(vm.uiState.value.submitError)
            assertFalse(vm.uiState.value.submitting)
            assertFalse(vm.uiState.value.submitted)
        }

    @Test
    fun dismissClearsTheErrorAndFormStaysEditable() =
        runTest(main.testDispatcher) {
            api.submitReviewHandler = { _, _ -> throw api.httpError(500, "{}") }
            val vm = vm()
            runCurrent()
            vm.submit()
            runCurrent()
            vm.dismissSubmitError()
            assertNull(vm.uiState.value.submitError)
            vm.onRatingSelected(1)
            vm.onCommentChanged("again")
            assertEquals(1, vm.uiState.value.rating)
            assertEquals("again", vm.uiState.value.comment)
        }

    @Test
    fun uiStateToStringNeverPrintsTheComment() {
        val text = WriteReviewUiState(comment = "secret words").toString()
        assertFalse(text.contains("secret"))
        assertTrue(text.contains("commentLength=12"))
    }
}
