@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Instant

class CategoriesSheetViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val categories =
        listOf(
            CategoryItem(id = "ai", name = "AI"),
            CategoryItem(id = "design", name = "Design"),
            CategoryItem(id = "music", name = "Music"),
        )

    private class Setup(
        val api: FakeClickMeApi,
        val store: CategoryStore,
    )

    private fun TestScope.setup(): Setup {
        val api = FakeClickMeApi().also { it.categoriesHandler = { CategoriesPayload(categories) } }
        val dataStore = testPreferencesDataStore(backgroundScope, tmp)
        return Setup(api, CategoryStore(api, dataStore, MutableClock(Instant.parse("2026-10-21T12:00:00Z"))))
    }

    private fun viewModel(
        store: CategoryStore,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = CategoriesSheetViewModel(store, handle)

    @Test
    fun openSeedsDraftAndEnsureLoadedLoadsThroughStore() =
        runTest(main.testDispatcher) {
            val s = setup()
            val vm = viewModel(s.store)
            vm.open(setOf("ai"))
            vm.ensureLoaded()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(setOf("ai"), state.draft)
            assertEquals("", state.query)
            assertEquals(CategoryCatalog.Loaded(categories), state.catalog)
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun warmStoreServesFreshViewModelWithoutNetworkCall() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            val vm = viewModel(s.store)
            vm.ensureLoaded()
            runCurrent()

            assertEquals(CategoryCatalog.Loaded(categories), vm.uiState.value.catalog)
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun failureShowsFailedAndRetryRefreshesThroughStore() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.api.categoriesHandler = { throw IOException("offline") }
            val vm = viewModel(s.store)
            vm.ensureLoaded()
            runCurrent()
            assertEquals(CategoryCatalog.Failed, vm.uiState.value.catalog)
            assertEquals(1, s.api.categoriesCalls)

            s.api.categoriesHandler = { CategoriesPayload(categories) }
            vm.retry()
            assertEquals(CategoryCatalog.Loading, vm.uiState.value.catalog)
            runCurrent()

            assertEquals(CategoryCatalog.Loaded(categories), vm.uiState.value.catalog)
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun retryBypassesWarmCache() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            val vm = viewModel(s.store)
            vm.retry()
            runCurrent()

            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun toggleAddsAndRemovesAndClearEmptiesDraftOnly() =
        runTest(main.testDispatcher) {
            val s = setup()
            val vm = viewModel(s.store)
            vm.open(setOf("ai"))
            vm.ensureLoaded()
            runCurrent()

            vm.toggle("design")
            assertEquals(setOf("ai", "design"), vm.uiState.value.draft)
            vm.toggle("ai")
            assertEquals(setOf("design"), vm.uiState.value.draft)

            vm.clear()
            assertEquals(emptySet<String>(), vm.uiState.value.draft)
            assertEquals(CategoryCatalog.Loaded(categories), vm.uiState.value.catalog)
        }

    @Test
    fun queryFiltersLocallyCaseInsensitivelyInServerOrder() =
        runTest(main.testDispatcher) {
            val s = setup()
            val vm = viewModel(s.store)
            vm.ensureLoaded()
            runCurrent()

            vm.onQueryChange("DES")
            val state = vm.uiState.value
            assertEquals(listOf("Design"), state.visible.map { it.name })
            assertFalse(state.noMatch)

            vm.onQueryChange("zzz")
            val noMatchState = vm.uiState.value
            assertTrue(noMatchState.visible.isEmpty())
            assertTrue(noMatchState.noMatch)

            vm.onQueryChange("  ")
            assertEquals(categories, vm.uiState.value.visible)
            assertFalse(vm.uiState.value.noMatch)
            // Search never reaches the server: still the single load call.
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun draftAndQueryRestoreFromSavedStateHandle() =
        runTest(main.testDispatcher) {
            val s = setup()
            val handle =
                SavedStateHandle(
                    mapOf(
                        "categories_sheet_draft" to arrayListOf("ai"),
                        "categories_sheet_query" to "de",
                    ),
                )
            val vm = viewModel(s.store, handle)

            assertEquals(setOf("ai"), vm.uiState.value.draft)
            assertEquals("de", vm.uiState.value.query)
        }

    @Test
    fun mutationsPersistToSavedStateHandle() =
        runTest(main.testDispatcher) {
            val s = setup()
            val handle = SavedStateHandle()
            val vm = viewModel(s.store, handle)
            vm.open(setOf("music", "ai"))
            vm.onQueryChange("mu")
            vm.toggle("design")

            assertEquals(arrayListOf("ai", "design", "music"), handle.get<ArrayList<String>>("categories_sheet_draft"))
            assertEquals("mu", handle.get<String>("categories_sheet_query"))
        }

    @Test
    fun ensureLoadedWhenAlreadyLoadedMakesNoStoreCall() =
        runTest(main.testDispatcher) {
            val s = setup()
            val vm = viewModel(s.store)
            vm.ensureLoaded()
            runCurrent()
            vm.ensureLoaded()
            runCurrent()

            assertEquals(1, s.api.categoriesCalls)
            assertEquals(CategoryCatalog.Loaded(categories), vm.uiState.value.catalog)
        }
}
