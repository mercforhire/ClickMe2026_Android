@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDto
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileStatsDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewItemDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewSummaryDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewerDto
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.toggleFavorite
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ExpertProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private val fullKey =
        ExpertProfileKey(
            expertId = "e1",
            name = "Ada",
            title = "Engineer",
            imageUrl = "u",
            rating = 4.5,
            reviewCount = 12,
            tags = listOf("AI"),
        )

    private val topicKey = ExpertProfileKey(expertId = "e1", name = "Ada", title = "", imageUrl = null)

    private fun vm(key: ExpertProfileKey = fullKey) =
        ExpertProfileViewModel(key, ExpertProfileRepository(api), FavoritesRepository(api))

    private fun details(
        name: String = "Ada Lovelace",
        title: String? = "Principal Engineer",
        bio: String? = "Bio",
        isBookable: Boolean? = null,
        topics: List<TopicOfDiscussionDto> = emptyList(),
        isFavorite: Boolean = false,
    ) = PublicProfileDetailsData(
        isBookable = isBookable,
        isFavorite = isFavorite,
        profile =
            PublicProfileDto(
                name = name,
                title = title,
                bio = bio,
                stats = PublicProfileStatsDto(experienceYears = 9, totalBookings = 40),
            ),
        topicsOfDiscussion = topics,
    )

    private fun reviews(
        overall: Double?,
        total: Int,
        items: List<ReviewItemDto> = emptyList(),
    ) = ExpertReviewsPayload(
        summary = ReviewSummaryDto(overallRating = overall, totalReviews = total),
        reviews = items,
    )

    private fun review(
        id: String,
        name: String? = "Sam",
    ) = ReviewItemDto(id = id, reviewer = ReviewerDto(name = name), rating = 5, comment = "Great")

    private val notFound = """{"status":"error","code":"NOT_FOUND","message":"Expert not found"}"""

    private fun neverCompleting() {
        api.expertDetailsHandler = { CompletableDeferred<PublicProfileDetailsData>().await() }
        api.expertReviewsHandler = { CompletableDeferred<ExpertReviewsPayload>().await() }
    }

    @Test
    fun initialStateIsThePreviewFromTheKey() =
        runTest(main.testDispatcher) {
            neverCompleting()
            val state = vm().uiState.value
            assertEquals("e1", state.expertId)
            assertEquals("Ada", state.name)
            assertEquals("Engineer", state.title)
            assertEquals("u", state.imageUrl)
            assertEquals(listOf("AI"), state.tags)
            assertEquals(HeroRating.Shown(4.5, 12), state.rating)
            assertEquals(DetailsState.Loading, state.details)
            assertEquals(ReviewsState.Loading, state.reviews)
        }

    @Test
    fun featuredTopicKeyHasPendingRatingAndPendingTitle() =
        runTest(main.testDispatcher) {
            neverCompleting()
            val state = vm(topicKey).uiState.value
            assertEquals(HeroRating.Pending, state.rating)
            assertTrue(state.isTitlePending)
        }

    @Test
    fun seedWithZeroReviewsIsNoReviews() =
        runTest(main.testDispatcher) {
            neverCompleting()
            assertEquals(HeroRating.NoReviews, vm(fullKey.copy(reviewCount = 0)).uiState.value.rating)
        }

    @Test
    fun detailsAndReviewsStartInParallelBeforeEitherCompletes() =
        runTest(main.testDispatcher) {
            val details = CompletableDeferred<PublicProfileDetailsData>()
            val reviews = CompletableDeferred<ExpertReviewsPayload>()
            api.expertDetailsHandler = { details.await() }
            api.expertReviewsHandler = { reviews.await() }
            vm()
            runCurrent()
            assertEquals(1, api.expertDetailsCalls)
            assertEquals(1, api.expertReviewsCalls)
        }

    @Test
    fun detailsSuccessMergesNameTitleAndMapsTopics() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                details(
                    topics =
                        listOf(
                            TopicOfDiscussionDto("t1", "Free chat", 30, "d", null, TopicPriceDto(null, null, true)),
                            TopicOfDiscussionDto("t2", "Deep dive", 60, null, null, TopicPriceDto(5000, "USD", false)),
                            TopicOfDiscussionDto("t3", "Mystery", null, null, null, TopicPriceDto(null, null, false)),
                        ),
                )
            }
            api.expertReviewsHandler = { reviews(null, 0) }
            val vm = vm()
            runCurrent()
            val state = vm.uiState.value
            assertEquals("Ada Lovelace", state.name)
            assertEquals("Principal Engineer", state.title)
            val loaded = (state.details as DetailsState.Loaded).details
            assertEquals(9, loaded.experienceYears)
            assertEquals(40, loaded.totalBookings)
            assertEquals("Bio", loaded.bio)
            assertEquals(PriceDisplay.Free, loaded.topics[0].price)
            assertTrue(loaded.topics[1].price is PriceDisplay.Paid)
            assertEquals(PriceDisplay.Unknown, loaded.topics[2].price)
            assertEquals(listOf("t1", "t2", "t3"), loaded.topics.map { it.id })
            assertEquals(30, loaded.topics[0].durationMins)
            assertEquals("d", loaded.topics[0].description)
        }

    @Test
    fun blankDetailsNameAndTitleKeepTheSeed() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details(name = "", title = null) }
            api.expertReviewsHandler = { reviews(null, 0) }
            val vm = vm()
            runCurrent()
            assertEquals("Ada", vm.uiState.value.name)
            assertEquals("Engineer", vm.uiState.value.title)
        }

    @Test
    fun topicKeyTitleComesFromDetailsAndIsNoLongerPending() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { reviews(null, 0) }
            val vm = vm(topicKey)
            runCurrent()
            assertEquals("Principal Engineer", vm.uiState.value.title)
            assertEquals(false, vm.uiState.value.isTitlePending)
        }

    @Test
    fun nullIsBookableMeansBookableAndFalseMeansNot() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { reviews(null, 0) }
            api.expertDetailsHandler = { details(isBookable = null) }
            val a = vm()
            runCurrent()
            assertTrue(((a.uiState.value.details) as DetailsState.Loaded).details.isBookable)

            api.expertDetailsHandler = { details(isBookable = false) }
            val b = vm()
            runCurrent()
            assertEquals(false, ((b.uiState.value.details) as DetailsState.Loaded).details.isBookable)
        }

    @Test
    fun reviewsSummaryReplacesTheSeedRatingAndMapsRows() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = {
                reviews(4.8, 7, listOf(review("r1"), review("r2", name = null), review("r3"), review("r4")))
            }
            val vm = vm()
            runCurrent()
            val state = vm.uiState.value
            assertEquals(HeroRating.Shown(4.8, 7), state.rating)
            val rows = (state.reviews as ReviewsState.Loaded).items
            assertEquals(3, rows.size)
            assertEquals("Sam", rows[0].author)
            assertEquals(null, rows[1].author)
            assertEquals("Great", rows[0].comment)
        }

    @Test
    fun zeroReviewsGivesNoReviews() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { reviews(null, 0) }
            val vm = vm()
            runCurrent()
            assertEquals(HeroRating.NoReviews, vm.uiState.value.rating)
            assertEquals(ReviewsState.Loaded(emptyList()), vm.uiState.value.reviews)
        }

    @Test
    fun nullOverallRatingWithReviewsKeepsSeedValueOrZero() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { reviews(null, 5) }
            val seeded = vm()
            val unseeded = vm(topicKey)
            runCurrent()
            assertEquals(HeroRating.Shown(4.5, 5), seeded.uiState.value.rating)
            assertEquals(HeroRating.Shown(0.0, 5), unseeded.uiState.value.rating)
        }

    @Test
    fun reviewsFailureKeepsSeedRatingAndLeavesDetailsAlone() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { throw api.httpError(500, """{"status":"error","message":"boom"}""") }
            val vm = vm()
            runCurrent()
            val state = vm.uiState.value
            assertEquals(ReviewsState.Failed, state.reviews)
            assertEquals(HeroRating.Shown(4.5, 12), state.rating)
            assertTrue(state.details is DetailsState.Loaded)
        }

    @Test
    fun reviewsFailureWithoutSeedHidesTheRatingLine() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { throw IOException("offline") }
            val vm = vm(topicKey)
            runCurrent()
            assertEquals(ReviewsState.Failed, vm.uiState.value.reviews)
            assertEquals(HeroRating.Hidden, vm.uiState.value.rating)
        }

    @Test
    fun details404IsUnavailableAndReviewsStillLoad() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw api.httpError(404, notFound) }
            api.expertReviewsHandler = { reviews(4.8, 7, listOf(review("r1"))) }
            val vm = vm()
            runCurrent()
            assertEquals(DetailsState.Unavailable, vm.uiState.value.details)
            assertTrue(vm.uiState.value.reviews is ReviewsState.Loaded)
        }

    @Test
    fun details500AndNetworkFailureAreErrors() =
        runTest(main.testDispatcher) {
            api.expertReviewsHandler = { reviews(null, 0) }
            api.expertDetailsHandler = { throw api.httpError(500, """{"status":"error","message":"boom"}""") }
            val a = vm()
            runCurrent()
            assertEquals(DetailsState.Error, a.uiState.value.details)

            api.expertDetailsHandler = { throw IOException("offline") }
            val b = vm()
            runCurrent()
            assertEquals(DetailsState.Error, b.uiState.value.details)
        }

    @Test
    fun retryResendsBothRequestsAndRecovers() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw api.httpError(500, """{"status":"error","message":"boom"}""") }
            api.expertReviewsHandler = { reviews(4.8, 7) }
            val vm = vm()
            runCurrent()
            assertEquals(DetailsState.Error, vm.uiState.value.details)

            api.expertDetailsHandler = { details() }
            vm.retry()
            assertEquals(DetailsState.Loading, vm.uiState.value.details)
            runCurrent()
            assertEquals(2, api.expertDetailsCalls)
            assertEquals(2, api.expertReviewsCalls)
            assertTrue(vm.uiState.value.details is DetailsState.Loaded)
        }

    @Test
    fun bookingKeyCarriesCurrentNameTitleAndImage() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { details() }
            api.expertReviewsHandler = { reviews(null, 0) }
            val vm = vm()
            assertEquals(BookingKey("e1", "Ada", "Engineer", "u"), vm.bookingKey())
            runCurrent()
            assertEquals(BookingKey("e1", "Ada Lovelace", "Principal Engineer", "u"), vm.bookingKey())
        }

    private fun loadedVm(isFavorite: Boolean): ExpertProfileViewModel {
        api.expertDetailsHandler = { details(isFavorite = isFavorite) }
        api.expertReviewsHandler = { reviews(null, 0) }
        return vm()
    }

    @Test
    fun heartStartsFromDetails() =
        runTest(main.testDispatcher) {
            neverCompleting()
            val pending = vm()
            runCurrent()
            assertEquals(false, pending.uiState.value.isFavorite)

            val on = loadedVm(isFavorite = true)
            runCurrent()
            assertEquals(true, on.uiState.value.isFavorite)

            val off = loadedVm(isFavorite = false)
            runCurrent()
            assertEquals(false, off.uiState.value.isFavorite)
        }

    @Test
    fun tapWhenNotFavoritedFlipsAtOnceAndCallsPut() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ToggleFavoriteData>()
            api.addFavoriteHandler = { gate.await() }
            val vm = loadedVm(isFavorite = false)
            runCurrent()

            vm.onToggleFavorite()
            assertEquals(true, vm.uiState.value.isFavorite)
            runCurrent()
            assertEquals(listOf("e1"), api.addFavoriteRequests)
            assertEquals(0, api.removeFavoriteCalls)
            assertEquals(true, vm.uiState.value.isFavorite)

            gate.complete(toggleFavorite("e1", isFavorite = true))
            runCurrent()
            assertEquals(true, vm.uiState.value.isFavorite)
        }

    @Test
    fun tapWhenFavoritedCallsDelete() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ToggleFavoriteData>()
            api.removeFavoriteHandler = { gate.await() }
            val vm = loadedVm(isFavorite = true)
            runCurrent()

            vm.onToggleFavorite()
            assertEquals(false, vm.uiState.value.isFavorite)
            runCurrent()
            assertEquals(listOf("e1"), api.removeFavoriteRequests)
            assertEquals(0, api.addFavoriteCalls)
            gate.complete(toggleFavorite("e1", isFavorite = false))
            runCurrent()
            assertEquals(false, vm.uiState.value.isFavorite)
        }

    @Test
    fun serverResultWins() =
        runTest(main.testDispatcher) {
            api.addFavoriteHandler = { toggleFavorite("e1", isFavorite = false) }
            val vm = loadedVm(isFavorite = false)
            runCurrent()

            vm.onToggleFavorite()
            runCurrent()
            assertEquals(false, vm.uiState.value.isFavorite)
        }
}
