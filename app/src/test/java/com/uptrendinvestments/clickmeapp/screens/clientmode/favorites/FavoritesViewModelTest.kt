@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.model.responses.FavoritesPayload
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.favoriteItem
import com.uptrendinvestments.clickmeapp.testing.favoritesPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = BookingsLaunchRequest()

    private fun viewModel() = FavoritesViewModel(FavoritesRepository(api), launchRequest)

    private fun stub(vararg ids: String) {
        api.getClientFavoritesHandler = { _, _ -> favoritesPayload(*ids.map { favoriteItem(it) }.toTypedArray()) }
    }

    private fun ui(vararg ids: String) = FavoritesLoad.Loaded(ids.map { favoriteItem(it).toUi() })

    // ---- Task 1: first load -------------------------------------------------------------------------------------

    @Test
    fun firstShowLoadsOnceWithPageOneLimitHundred() =
        runTest(main.testDispatcher) {
            stub("a", "b")
            val vm = viewModel()
            runCurrent()
            assertEquals(0, api.getClientFavoritesCalls)
            assertEquals(FavoritesLoad.Loading, vm.uiState.value.load)

            vm.onScreenShown()
            runCurrent()

            assertEquals(1, api.getClientFavoritesCalls)
            assertEquals(listOf(1 to 100), api.clientFavoritesQueries)
            assertEquals(ui("a", "b"), vm.uiState.value.load)
        }

    @Test
    fun secondShowWhileTheFirstLoadRunsDoesNothing() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            api.getClientFavoritesHandler = { _, _ -> gate.await() }
            val vm = viewModel()

            vm.onScreenShown()
            runCurrent()
            vm.onScreenShown()
            runCurrent()

            assertEquals(1, api.getClientFavoritesCalls)
        }

    @Test
    fun mapsRowsWithIosFallbacks() =
        runTest(main.testDispatcher) {
            api.getClientFavoritesHandler = { _, _ ->
                favoritesPayload(
                    favoriteItem("a", name = null, headline = null, tags = null, avatarUrl = null),
                    favoriteItem("b", name = "  ", tags = listOf("AI", " ", "ML")),
                )
            }
            val vm = viewModel()

            vm.onScreenShown()
            runCurrent()

            val rows = (vm.uiState.value.load as FavoritesLoad.Loaded).experts
            assertEquals(FavoriteExpertUi("a", null, "", emptyList(), null), rows[0])
            assertEquals(null, rows[1].name)
            assertEquals(listOf("AI", "ML"), rows[1].tags)
        }
}
