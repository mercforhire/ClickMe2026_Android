@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertsSearchPayload
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.SearchExpertsQuery
import com.uptrendinvestments.clickmeapp.testing.searchCategoryItems
import com.uptrendinvestments.clickmeapp.testing.searchPayload
import com.uptrendinvestments.clickmeapp.testing.searchResultItem
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Instant

class SearchExpertViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeClickMeApi()

    private val boom = """{"status":"error","code":"INTERNAL_ERROR","message":"Boom"}"""

    private fun TestScope.viewModel(): SearchExpertViewModel {
        val store =
            CategoryStore(
                api,
                testPreferencesDataStore(backgroundScope, tmp),
                MutableClock(Instant.parse("2026-10-30T12:00:00Z")),
            )
        return SearchExpertViewModel(SearchRepository(api), store)
    }

    private fun stubBrowse() {
        api.searchExpertsHandler = { searchPayload(searchResultItem("e1"), searchResultItem("e2")) }
        api.categoriesHandler = { CategoriesPayload(searchCategoryItems()) }
    }

    // ---- Task 1: first load -------------------------------------------------------------------------------------

    @Test
    fun firstLoadSendsTheBrowseSetOnce() =
        runTest(main.testDispatcher) {
            stubBrowse()
            val vm = viewModel()
            assertEquals(SearchResults.Loading, vm.uiState.value.results)

            runCurrent()

            assertEquals(listOf(SearchExpertsQuery("", null, "relevance", 1, 20)), api.searchExpertsQueries)
            val expected = listOf(searchResultItem("e1"), searchResultItem("e2")).map { it.toSearchExpertUi() }
            assertEquals(SearchResults.Loaded(expected), vm.uiState.value.results)

            advanceUntilIdle()
            assertEquals(1, api.searchExpertsCalls)
        }

    @Test
    fun categoriesLoadInParallelAsChips() =
        runTest(main.testDispatcher) {
            stubBrowse()
            val vm = viewModel()
            runCurrent()

            assertEquals(
                listOf(SearchCategoryChipUi("business", "Business"), SearchCategoryChipUi("design", "Design")),
                vm.uiState.value.categories,
            )
            assertEquals(1, api.categoriesCalls)
        }

    @Test
    fun categoriesFailureLeavesOnlyAllAndNeverBlocksResults() =
        runTest(main.testDispatcher) {
            stubBrowse()
            api.categoriesHandler = { throw IOException() }
            val vm = viewModel()
            runCurrent()

            assertEquals(emptyList<SearchCategoryChipUi>(), vm.uiState.value.categories)
            assertTrue(vm.uiState.value.results is SearchResults.Loaded)
        }

    @Test
    fun slowCategoriesDoNotDelayResults() =
        runTest(main.testDispatcher) {
            stubBrowse()
            val never = CompletableDeferred<CategoriesPayload>()
            api.categoriesHandler = { never.await() }
            val vm = viewModel()
            runCurrent()

            assertTrue(vm.uiState.value.results is SearchResults.Loaded)
            assertEquals(emptyList<SearchCategoryChipUi>(), vm.uiState.value.categories)
        }

    @Test
    fun firstLoadFailureShowsServerMessage() =
        runTest(main.testDispatcher) {
            stubBrowse()
            api.searchExpertsHandler = { throw api.httpError(500, boom) }
            val vm = viewModel()
            runCurrent()

            assertEquals(SearchResults.Failed(FieldError.Text("Boom")), vm.uiState.value.results)
        }

    @Test
    fun firstLoadNetworkFailureShowsGenericCopy() =
        runTest(main.testDispatcher) {
            stubBrowse()
            api.searchExpertsHandler = { throw IOException() }
            val vm = viewModel()
            runCurrent()

            assertEquals(
                SearchResults.Failed(FieldError.Res(R.string.error_state_body)),
                vm.uiState.value.results,
            )
        }

    // ---- Task 2: user triggers ----------------------------------------------------------------------------------

    private fun TestScope.loadedViewModel(): SearchExpertViewModel {
        stubBrowse()
        return viewModel().also { runCurrent() }
    }

    /** Makes every further search suspend until the returned deferred completes. */
    private fun suspendSearches(): CompletableDeferred<ExpertsSearchPayload> {
        val gate = CompletableDeferred<ExpertsSearchPayload>()
        api.searchExpertsHandler = { gate.await() }
        return gate
    }

    private fun lastQuery(): SearchExpertsQuery = api.searchExpertsQueries.last()

    @Test
    fun textIsDebouncedOnTheTrimmedQuery() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onQueryChange("  ada ")
            assertEquals("  ada ", vm.uiState.value.query)
            advanceTimeBy(299)
            runCurrent()
            assertEquals(1, api.searchExpertsCalls)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)
            assertEquals(SearchExpertsQuery("ada", null, "relevance", 1, 20), lastQuery())
        }

    @Test
    fun typingAgainRestartsTheDebounce() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onQueryChange("a")
            advanceTimeBy(200)
            vm.onQueryChange("ad")
            advanceTimeBy(200)
            runCurrent()
            assertEquals(1, api.searchExpertsCalls)

            advanceTimeBy(100)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)
            assertEquals("ad", lastQuery().q)
        }

    @Test
    fun trailingSpaceReSearchesLikeIos() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onQueryChange("ada")
            advanceTimeBy(300)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)

            vm.onQueryChange("ada ")
            advanceTimeBy(300)
            runCurrent()
            assertEquals(3, api.searchExpertsCalls)
            assertEquals("ada", lastQuery().q)
        }

    @Test
    fun chipTapRunsImmediatelyAndCancelsPendingDebounce() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onQueryChange("ada")
            advanceTimeBy(100)
            vm.onCategorySelected("design")
            runCurrent()

            assertEquals(2, api.searchExpertsCalls)
            assertEquals(SearchExpertsQuery("ada", "design", "relevance", 1, 20), lastQuery())
            assertEquals("design", vm.uiState.value.selectedCategorySlug)

            advanceTimeBy(300)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)
        }

    @Test
    fun allChipSendsNoCategoryAndReTapReRuns() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onCategorySelected(ALL_CATEGORIES_SLUG)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)
            assertEquals(null, lastQuery().category)

            vm.onCategorySelected("design")
            runCurrent()
            vm.onCategorySelected("design")
            runCurrent()
            assertEquals(4, api.searchExpertsCalls)
            assertEquals("design", lastQuery().category)
            assertEquals("design", vm.uiState.value.selectedCategorySlug)
        }

    @Test
    fun sortPickRunsImmediatelyWithApiValue() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()

            vm.onSortSelected(SearchSort.HighestRated)
            runCurrent()
            assertEquals(2, api.searchExpertsCalls)
            assertEquals("rating", lastQuery().sort)
            assertEquals(SearchSort.HighestRated, vm.uiState.value.sort)

            vm.onSortSelected(SearchSort.MostPopular)
            runCurrent()
            assertEquals("relevance", lastQuery().sort)
        }

    @Test
    fun latestSearchWins() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            val gateA = CompletableDeferred<ExpertsSearchPayload>()
            api.searchExpertsHandler = { query ->
                when (query.q) {
                    "a" -> gateA.await()
                    else -> searchPayload(searchResultItem("b1"))
                }
            }

            vm.onQueryChange("a")
            advanceTimeBy(300)
            runCurrent()
            vm.onQueryChange("b")
            advanceTimeBy(300)
            runCurrent()
            val expectedB = SearchResults.Loaded(listOf(searchResultItem("b1").toSearchExpertUi()))
            assertEquals(expectedB, vm.uiState.value.results)

            gateA.complete(searchPayload(searchResultItem("a1")))
            advanceUntilIdle()
            assertEquals(expectedB, vm.uiState.value.results)
        }

    @Test
    fun everyNewSearchShowsLoadingFirst() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            assertTrue(vm.uiState.value.results is SearchResults.Loaded)

            val chipGate = suspendSearches()
            vm.onCategorySelected("design")
            runCurrent()
            assertEquals(SearchResults.Loading, vm.uiState.value.results)

            chipGate.complete(searchPayload(searchResultItem("e1")))
            runCurrent()
            assertTrue(vm.uiState.value.results is SearchResults.Loaded)

            suspendSearches()
            vm.onQueryChange("zed")
            advanceTimeBy(300)
            runCurrent()
            assertEquals(SearchResults.Loading, vm.uiState.value.results)
        }
}
