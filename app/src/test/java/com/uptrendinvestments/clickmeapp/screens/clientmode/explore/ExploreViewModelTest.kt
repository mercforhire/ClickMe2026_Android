@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.FeaturedTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.FeaturedTopicExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.MoneyDto
import com.uptrendinvestments.clickmeapp.model.responses.RecommendedExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.TrendingCategoryDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class ExploreViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val now = Instant.parse("2026-10-21T16:00:00Z")
    private val clock = MutableClock(now, ZoneId.systemDefault())

    private val boom = """{"status":"error","code":"INTERNAL_ERROR","message":"Boom"}"""

    private fun expert(
        id: String,
        rating: Double? = 4.5,
        reviews: Int? = 3,
        tags: List<String>? = listOf("tax"),
    ) = RecommendedExpertDto(
        expertId = id,
        fullName = "Expert $id",
        title = "Title $id",
        profileImageUrl = "https://img/$id",
        rating = rating,
        reviewCount = reviews,
        expertiseTags = tags,
    )

    private fun topic(
        id: String,
        isFree: Boolean? = false,
        amount: Long? = 5000,
        currency: String? = "CAD",
    ) = FeaturedTopicDto(
        topicId = id,
        title = "Topic $id",
        hourlyRate = MoneyDto(amount, currency),
        isFree = isFree,
        expert = FeaturedTopicExpertDto("fx-$id", "Featured $id", null),
    )

    private fun home(
        experts: List<RecommendedExpertDto> = listOf(expert("e1")),
        topics: List<FeaturedTopicDto>? = listOf(topic("t1")),
    ) = ClientHomeData(
        trendingCategories = listOf(TrendingCategoryDto("ai", "AI"), TrendingCategoryDto("design", "Design")),
        recommendedExperts = experts,
        featuredTopics = topics,
    )

    private fun booking(
        id: String,
        start: Instant = now,
        status: String = "confirmed",
    ) = ClientBookingItem(
        bookingId = id,
        expert = BookingExpertDto(id = "x-$id", fullName = "Coach $id", avatarUrl = "https://a/$id"),
        topic = "Topic",
        startTime = start,
        endTime = start.plusSeconds(3600),
        meetingType = "video",
        status = status,
    )

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        ExploreViewModel(ExploreRepository(api), ClientBookingsRepository(api), clock, saved)

    private fun stubDefaults() {
        api.clientHomeHandler = { home() }
        api.clientBookingsHandler = { ClientBookingsPayload(bookings = listOf(booking("b1"))) }
    }

    @Test
    fun initLoadsAllSectionsAndTodaysSessions() =
        runTest {
            stubDefaults()
            api.clientBookingsHandler = {
                ClientBookingsPayload(
                    bookings =
                        listOf(
                            booking("b1"),
                            booking("later", now.plusSeconds(2L * 24 * 3600)),
                            booking("pending", status = "pending_approval"),
                        ),
                )
            }
            val vm = viewModel()
            assertEquals(SectionState.Loading, vm.uiState.value.categories)
            assertEquals(SectionState.Loading, vm.uiState.value.featuredTopics)
            assertEquals(SectionState.Loading, vm.uiState.value.experts)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(listOf<String?>(null), api.clientHomeCategories)
            assertEquals(
                listOf(CategoryChipUi("ai", "AI"), CategoryChipUi("design", "Design")),
                (state.categories as SectionState.Loaded).data,
            )
            assertEquals(1, (state.featuredTopics as SectionState.Loaded).data.size)
            assertEquals(listOf("e1"), (state.experts as SectionState.Loaded).data.map { it.expertId })
            assertEquals(listOf("b1"), state.todaysSessions.map { it.bookingId })
            assertEquals(
                formatSessionTimeRange(now, now.plusSeconds(3600), ZoneId.systemDefault(), Locale.getDefault()),
                state.todaysSessions.single().timeRange,
            )
            assertFalse(state.hasFilter)
        }

    @Test
    fun homeHttpErrorPutsEverySectionInErrorWithServerMessage() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = { throw api.httpError(500, boom) }
            val vm = viewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(SectionState.Error(FieldError.Text("Boom")), state.categories)
            assertEquals(SectionState.Error(FieldError.Text("Boom")), state.featuredTopics)
            assertEquals(SectionState.Error(FieldError.Text("Boom")), state.experts)
            assertEquals(listOf("b1"), state.todaysSessions.map { it.bookingId })
        }

    @Test
    fun homeNetworkErrorUsesGenericMessage() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = { throw IOException("offline") }
            val vm = viewModel()
            advanceUntilIdle()

            val expected = SectionState.Error(FieldError.Res(R.string.error_state_body))
            assertEquals(expected, vm.uiState.value.categories)
            assertEquals(expected, vm.uiState.value.featuredTopics)
            assertEquals(expected, vm.uiState.value.experts)
        }

    @Test
    fun bookingsFailureLeavesHomeLoadedAndStripEmpty() =
        runTest {
            stubDefaults()
            api.clientBookingsHandler = { throw api.httpError(500, boom) }
            val vm = viewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.todaysSessions.isEmpty())
            assertTrue(state.categories is SectionState.Loaded)
            assertTrue(state.featuredTopics is SectionState.Loaded)
            assertTrue(state.experts is SectionState.Loaded)
        }

    @Test
    fun retryAfterHomeFailureResendsHomeOnlyAndFillsSections() =
        runTest {
            stubDefaults()
            var fail = true
            api.clientHomeHandler = { if (fail) throw IOException("offline") else home() }
            val vm = viewModel()
            advanceUntilIdle()
            val bookingsBefore = api.clientBookingsCalls

            fail = false
            vm.onRetry()
            advanceUntilIdle()

            assertEquals(listOf<String?>(null, null), api.clientHomeCategories)
            assertEquals(bookingsBefore, api.clientBookingsCalls)
            assertTrue(vm.uiState.value.categories is SectionState.Loaded)
            assertTrue(vm.uiState.value.featuredTopics is SectionState.Loaded)
            assertTrue(vm.uiState.value.experts is SectionState.Loaded)
        }

    @Test
    fun retryKeepsTheCurrentCategory() =
        runTest {
            stubDefaults()
            val vm = viewModel(SavedStateHandle(mapOf(ExploreViewModel.KEY_SELECTION to arrayListOf("ai"))))
            advanceUntilIdle()
            api.clientHomeHandler = { throw IOException("offline") }
            vm.onChipTap("design")
            advanceUntilIdle()
            assertTrue(vm.uiState.value.experts is SectionState.Error)

            api.clientHomeHandler = { home() }
            vm.onRetry()
            advanceUntilIdle()

            assertEquals(listOf<String?>("ai", "design", "design"), api.clientHomeCategories)
            assertTrue(vm.uiState.value.experts is SectionState.Loaded)
        }

    @Test
    fun chipTapSelectsExactlyThatSlugAndOnlyReloadsExperts() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            val before = vm.uiState.value

            api.clientHomeHandler = { home(experts = listOf(expert("e2"))) }
            vm.onChipTap("ai")
            assertEquals(setOf("ai"), vm.uiState.value.selection)
            assertEquals(SectionState.Loading, vm.uiState.value.experts)
            advanceUntilIdle()

            val after = vm.uiState.value
            assertEquals("ai", api.clientHomeCategories.last())
            assertEquals(listOf("e2"), (after.experts as SectionState.Loaded).data.map { it.expertId })
            assertSame(before.featuredTopics, after.featuredTopics)
            assertSame(before.categories, after.categories)
            assertTrue(after.hasFilter)
        }

    @Test
    fun tappingTheSelectedChipClearsTheFilter() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            vm.onChipTap("ai")
            advanceUntilIdle()

            vm.onChipTap("ai")
            advanceUntilIdle()

            assertEquals(emptySet<String>(), vm.uiState.value.selection)
            assertEquals(null, api.clientHomeCategories.last())
        }

    @Test
    fun chipTapReplacesAMultiSelection() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            vm.applySelection(setOf("ai", "design"))
            advanceUntilIdle()

            vm.onChipTap("design")
            advanceUntilIdle()
            assertEquals(emptySet<String>(), vm.uiState.value.selection)

            vm.applySelection(setOf("ai", "design"))
            advanceUntilIdle()
            vm.onChipTap("tech")
            advanceUntilIdle()
            assertEquals(setOf("tech"), vm.uiState.value.selection)
            assertEquals("tech", api.clientHomeCategories.last())
        }

    @Test
    fun applySelectionSendsSortedCommaJoinedSlugs() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            vm.applySelection(setOf("design", "ai"))
            advanceUntilIdle()
            assertEquals("ai,design", api.clientHomeCategories.last())

            val calls = api.clientHomeCalls
            vm.applySelection(setOf("ai", "design"))
            advanceUntilIdle()
            assertEquals(calls, api.clientHomeCalls)

            vm.clearFilter()
            advanceUntilIdle()
            assertEquals(null, api.clientHomeCategories.last())
            assertEquals(calls + 1, api.clientHomeCalls)
        }

    @Test
    fun staleResponseNeverOverwritesANewerFilter() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            val gate = CompletableDeferred<Unit>()
            api.clientHomeHandler = { category ->
                if (category == "ai") {
                    gate.await()
                    home(experts = listOf(expert("stale")))
                } else {
                    home(experts = listOf(expert("fresh")))
                }
            }
            vm.onChipTap("ai")
            runCurrent()
            vm.onChipTap("design")
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(setOf("design"), vm.uiState.value.selection)
            assertEquals(listOf("fresh"), (vm.uiState.value.experts as SectionState.Loaded).data.map { it.expertId })
        }

    @Test
    fun emptyFilteredResultIsLoadedEmptyWithFilterActive() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            api.clientHomeHandler = { home(experts = emptyList()) }
            vm.onChipTap("ai")
            advanceUntilIdle()

            assertEquals(SectionState.Loaded(emptyList<ExpertCardUi>()), vm.uiState.value.experts)
            assertTrue(vm.uiState.value.hasFilter)
        }

    @Test
    fun applyAfterFailedHomeFillsEverySection() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = { throw IOException("offline") }
            val vm = viewModel()
            advanceUntilIdle()

            api.clientHomeHandler = { home() }
            vm.applySelection(setOf("ai"))
            advanceUntilIdle()

            assertTrue(vm.uiState.value.categories is SectionState.Loaded)
            assertTrue(vm.uiState.value.featuredTopics is SectionState.Loaded)
            assertTrue(vm.uiState.value.experts is SectionState.Loaded)
        }

    @Test
    fun failedInitialHomeKeepsSectionsAConcurrentApplyAlreadyFilled() =
        runTest {
            stubDefaults()
            val gate = CompletableDeferred<Unit>()
            api.clientHomeHandler = { category ->
                if (category == null) {
                    gate.await()
                    throw IOException("offline")
                } else {
                    home(experts = listOf(expert("filtered")))
                }
            }
            val vm = viewModel()
            runCurrent()
            // Apply while the initial home request is still in flight; the experts request wins the race.
            vm.applySelection(setOf("ai"))
            runCurrent()
            assertTrue(vm.uiState.value.categories is SectionState.Loaded)
            assertTrue(vm.uiState.value.featuredTopics is SectionState.Loaded)

            gate.complete(Unit)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.categories is SectionState.Loaded)
            assertTrue(state.featuredTopics is SectionState.Loaded)
            assertEquals(listOf("filtered"), (state.experts as SectionState.Loaded).data.map { it.expertId })
            assertFalse(state.refreshFailed)
        }

    @Test
    fun refreshReloadsHomeAndBookingsAndTracksInFlight() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            val homeBefore = api.clientHomeCalls
            val bookingsBefore = api.clientBookingsCalls

            val gate = CompletableDeferred<Unit>()
            api.clientHomeHandler = {
                gate.await()
                home()
            }
            vm.onRefresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.experts is SectionState.Loaded)

            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(homeBefore + 1, api.clientHomeCalls)
            assertEquals(bookingsBefore + 1, api.clientBookingsCalls)
        }

    @Test
    fun failedRefreshKeepsDataAndRaisesTheFlagOnce() =
        runTest {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            val before = vm.uiState.value

            api.clientHomeHandler = { throw IOException("offline") }
            vm.onRefresh()
            advanceUntilIdle()

            val after = vm.uiState.value
            assertEquals(before.categories, after.categories)
            assertEquals(before.featuredTopics, after.featuredTopics)
            assertEquals(before.experts, after.experts)
            assertTrue(after.refreshFailed)
            assertFalse(after.isRefreshing)

            vm.onRefreshFailureConsumed()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun featuredTopicPricesMapThroughPriceDisplay() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = {
                home(
                    topics =
                        listOf(
                            topic("free", isFree = true, amount = null, currency = null),
                            topic("paid", isFree = false, amount = 5000, currency = "CAD"),
                            topic("unknown", isFree = false, amount = null, currency = null),
                        ),
                )
            }
            val vm = viewModel()
            advanceUntilIdle()

            val prices = (vm.uiState.value.featuredTopics as SectionState.Loaded).data.map { it.price }
            assertEquals(PriceDisplay.Free, prices[0])
            assertTrue(prices[1] is PriceDisplay.Paid)
            assertEquals(PriceDisplay.Unknown, prices[2])
        }

    @Test
    fun missingExpertFieldsDefault() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = { home(experts = listOf(expert("e", rating = null, reviews = null, tags = null))) }
            val vm = viewModel()
            advanceUntilIdle()

            val card = (vm.uiState.value.experts as SectionState.Loaded).data.single()
            assertEquals(0.0, card.rating, 0.0)
            assertEquals(0, card.reviewCount)
            assertEquals(emptyList<String>(), card.tags)
        }

    @Test
    fun nullFeaturedTopicsBecomeAnEmptyList() =
        runTest {
            stubDefaults()
            api.clientHomeHandler = { home(topics = null) }
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(SectionState.Loaded(emptyList<FeaturedTopicUi>()), vm.uiState.value.featuredTopics)
        }

    @Test
    fun restoredSelectionIsUsedForTheFirstRequestAndPersisted() =
        runTest {
            stubDefaults()
            val saved = SavedStateHandle(mapOf(ExploreViewModel.KEY_SELECTION to arrayListOf("ai")))
            val vm = viewModel(saved)
            assertEquals(setOf("ai"), vm.uiState.value.selection)
            advanceUntilIdle()
            assertEquals(listOf<String?>("ai"), api.clientHomeCategories)

            vm.applySelection(setOf("design", "ai"))
            assertEquals(arrayListOf("ai", "design"), saved.get<ArrayList<String>>(ExploreViewModel.KEY_SELECTION))
        }
}
