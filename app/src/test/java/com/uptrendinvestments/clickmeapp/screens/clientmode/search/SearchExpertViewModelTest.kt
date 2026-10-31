@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
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
}
