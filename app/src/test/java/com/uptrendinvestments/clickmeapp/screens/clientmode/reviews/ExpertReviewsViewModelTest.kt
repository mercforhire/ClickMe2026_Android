@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ReviewItemDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewerDto
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.reviewItem
import com.uptrendinvestments.clickmeapp.testing.reviewsPayload
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

class ExpertReviewsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm() = ExpertReviewsViewModel(ExpertReviewsKey("e1", "Ada Lovelace"), ReviewsRepository(api))

    private fun serverDown() = api.httpError(500, """{"status":"error","code":"INTERNAL","message":"Server down"}""")

    @Test
    fun loadsOnCreationAndMapsRows() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { reviewsPayload(4.5, 12, listOf(reviewItem())) }
            val vm = vm()
            runCurrent()

            assertEquals(1, api.expertReviewsCalls)
            assertEquals(
                ExpertReviewsLoad.Loaded(
                    overallRating = 4.5,
                    totalReviews = 12,
                    reviews =
                        listOf(
                            ExpertReviewUi("r1", "Sam Lee", null, 5, "Career advice", "Oct 12, 2026", "Great session"),
                        ),
                ),
                vm.uiState.value.load,
            )
        }

    @Test
    fun nullOverallReadsZeroAndBlankFieldsAreNull() =
        runTest(main.testDispatcher) {
            val bare = ReviewItemDto(id = "r2", reviewer = ReviewerDto(name = "  "), rating = 3, comment = " ")
            api.expertReviewsHandler = { reviewsPayload(null, 1, listOf(bare)) }
            val vm = vm()
            runCurrent()

            val loaded = vm.uiState.value.load as ExpertReviewsLoad.Loaded
            assertEquals(0.0, loaded.overallRating, 0.0)
            val row = loaded.reviews.single()
            assertNull(row.reviewerName)
            assertNull(row.topic)
            assertNull(row.dateLabel)
            assertNull(row.comment)
        }

    @Test
    fun firstLoadFailureIsFailedWithServerMessage() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { throw serverDown() }
            val vm = vm()
            runCurrent()
            assertEquals(ExpertReviewsLoad.Failed(FieldError.Text("Server down")), vm.uiState.value.load)

            api.expertReviewsHandler = { throw IOException("offline") }
            vm.retry()
            runCurrent()
            assertEquals(
                ExpertReviewsLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
        }

    @Test
    fun retryReloadsFromFailed() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { throw serverDown() }
            val vm = vm()
            runCurrent()

            val gate = CompletableDeferred<ExpertReviewsPayload>()
            api.expertReviewsHandler = { gate.await() }
            vm.retry()
            assertEquals(ExpertReviewsLoad.Loading, vm.uiState.value.load)
            gate.complete(reviewsPayload(4.0, 1, listOf(reviewItem())))
            runCurrent()

            assertTrue(vm.uiState.value.load is ExpertReviewsLoad.Loaded)
            assertEquals(2, api.expertReviewsCalls)
        }

    @Test
    fun pullOverContentKeepsContentAndRaisesRefreshFailed() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value.load

            val gate = CompletableDeferred<ExpertReviewsPayload>()
            api.expertReviewsHandler = { gate.await() }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(before, vm.uiState.value.load)

            api.expertReviewsHandler = { throw serverDown() }
            vm.refresh()
            runCurrent()

            assertEquals(before, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)

            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun pullOnFailedBehavesAsFirstLoad() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { throw serverDown() }
            val vm = vm()
            runCurrent()

            api.expertReviewsHandler = { CompletableDeferred<ExpertReviewsPayload>().await() }
            vm.refresh()

            assertEquals(ExpertReviewsLoad.Loading, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun retryCancellingAPullClearsBothFlags() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
            val vm = vm()
            runCurrent()

            api.expertReviewsHandler = { CompletableDeferred<ExpertReviewsPayload>().await() }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)

            api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
            vm.retry()
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun titleRule() {
        assertEquals("Ada", reviewsTitleFirstName("Ada Lovelace"))
        assertEquals("Ada", reviewsTitleFirstName("  Ada  "))
        assertEquals("Ada", reviewsTitleFirstName("Ada  Lovelace"))
        assertNull(reviewsTitleFirstName(""))
        assertNull(reviewsTitleFirstName("   "))

        api.expertReviewsHandler = { CompletableDeferred<ExpertReviewsPayload>().await() }
        assertEquals("Ada", vm().uiState.value.titleFirstName)
    }
}
