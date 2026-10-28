package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.reviewContext
import com.uptrendinvestments.clickmeapp.testing.submitReviewData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReviewsRepositoryTest {
    private val api = FakeClickMeApi()
    private val repository = ReviewsRepository(api)

    @Test
    fun submitRecordsTrimmedBody() =
        runTest {
            api.submitReviewHandler = { _, _ -> submitReviewData() }

            repository.submit("b1", 5, " Nice ")

            assertEquals("b1" to SubmitReviewRequest(5, "Nice"), api.submittedReviews.single())
        }

    @Test
    fun contextRecordsBookingId() =
        runTest {
            api.reviewContextHandler = { reviewContext() }

            repository.context("b1")

            assertEquals(listOf("b1"), api.reviewContextRequests)
        }

    @Test
    fun ratingOutsideRangeIsRejected() =
        runTest {
            api.submitReviewHandler = { _, _ -> submitReviewData() }

            listOf(0, 6).forEach { rating ->
                try {
                    repository.submit("b1", rating, null)
                    fail("rating $rating should be rejected")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message.orEmpty().isNotEmpty())
                }
            }

            assertTrue(api.submittedReviews.isEmpty())
        }

    @Test
    fun fakeRecordsTokenCalls() =
        runTest {
            val request = RegisterDeviceTokenRequest("dev-1", "tok-1")

            val registered = api.registerDeviceToken(request)
            val deleted = api.deleteDeviceToken("dev-1")

            assertEquals(listOf(request), api.registeredDeviceTokens)
            assertEquals(listOf("dev-1"), api.deletedDeviceTokens)
            assertEquals("success", registered.status)
            assertEquals("success", deleted.status)
        }
}
