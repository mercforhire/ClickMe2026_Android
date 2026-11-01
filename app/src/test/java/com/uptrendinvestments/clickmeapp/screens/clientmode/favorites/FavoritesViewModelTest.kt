@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.FavoritesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.favoriteItem
import com.uptrendinvestments.clickmeapp.testing.favoritesPayload
import com.uptrendinvestments.clickmeapp.testing.toggleFavorite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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

    // ---- Task 2: load modes -------------------------------------------------------------------------------------

    private val boom = """{"status":"error","code":"INTERNAL_ERROR","message":"Server down"}"""

    private val scripted = ArrayDeque<suspend () -> FavoritesPayload>()

    /** Each GET takes the next scripted answer, so a test controls the first, second and later responses. */
    private fun script(vararg answers: suspend () -> FavoritesPayload) {
        scripted.addAll(answers)
        api.getClientFavoritesHandler = { _, _ -> scripted.removeFirst()() }
    }

    private fun rows(vararg ids: String): suspend () -> FavoritesPayload =
        {
            favoritesPayload(*ids.map { favoriteItem(it) }.toTypedArray())
        }

    private fun fails(error: () -> Throwable = { api.httpError(500, boom) }): suspend () -> FavoritesPayload =
        {
            throw error()
        }

    @Test
    fun firstLoadFailureIsFailedWithServerMessage() =
        runTest(main.testDispatcher) {
            script(fails())
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()
            assertEquals(FavoritesLoad.Failed(FieldError.Text("Server down")), vm.uiState.value.load)

            script(fails { IOException() })
            vm.retry()
            runCurrent()
            assertEquals(FavoritesLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)
        }

    @Test
    fun retryReloadsFromFailed() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script(fails(), { gate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.retry()
            runCurrent()
            assertEquals(FavoritesLoad.Loading, vm.uiState.value.load)

            gate.complete(favoritesPayload(favoriteItem("a")))
            runCurrent()
            assertEquals(ui("a"), vm.uiState.value.load)
            assertEquals(2, api.getClientFavoritesCalls)
        }

    @Test
    fun returnWhenLoadedReloadsQuietlyKeepingRows() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script(rows("a"), { gate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(ui("a"), vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(2, api.getClientFavoritesCalls)

            gate.complete(favoritesPayload(favoriteItem("a"), favoriteItem("b")))
            runCurrent()
            assertEquals(ui("a", "b"), vm.uiState.value.load)
        }

    @Test
    fun quietFailureKeepsRowsSilently() =
        runTest(main.testDispatcher) {
            script(rows("a"), fails())
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()

            assertEquals(ui("a"), vm.uiState.value.load)
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun showWhileCurrentLoadRunsDoesNotRestart() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script({ gate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.onScreenShown()
            runCurrent()

            assertEquals(1, api.getClientFavoritesCalls)
        }

    @Test
    fun showAfterHiddenRestartsTheLoad() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script({ gate.await() }, rows("a"))
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()

            assertEquals(2, api.getClientFavoritesCalls)
            assertEquals(ui("a"), vm.uiState.value.load)
        }

    @Test
    fun pullFailureKeepsRowsAndRaisesRefreshFailedOnce() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script(rows("a"), { gate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(ui("a"), vm.uiState.value.load)

            gate.completeExceptionally(api.httpError(500, boom))
            runCurrent()
            assertEquals(ui("a"), vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)

            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun pullOnFailedBehavesAsFirstLoad() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script(fails(), { gate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()

            vm.refresh()
            runCurrent()

            assertEquals(FavoritesLoad.Loading, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun retryCancellingAPullClearsBothFlags() =
        runTest(main.testDispatcher) {
            val pullGate = CompletableDeferred<FavoritesPayload>()
            val retryGate = CompletableDeferred<FavoritesPayload>()
            script(rows("a"), { pullGate.await() }, { retryGate.await() })
            val vm = viewModel()
            vm.onScreenShown()
            runCurrent()
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)

            vm.retry()
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertFalse(vm.uiState.value.refreshFailed)

            pullGate.complete(favoritesPayload(favoriteItem("late")))
            runCurrent()
            assertEquals(FavoritesLoad.Loading, vm.uiState.value.load)

            retryGate.complete(favoritesPayload(favoriteItem("b")))
            runCurrent()
            assertEquals(ui("b"), vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertFalse(vm.uiState.value.refreshFailed)
        }

    // ---- Task 3: remove, race guard, View My History ------------------------------------------------------------

    private fun loadedViewModel(vararg ids: String): FavoritesViewModel {
        val vm = viewModel()
        vm.onScreenShown()
        main.testDispatcher.scheduler.runCurrent()
        assertEquals(ui(*ids), vm.uiState.value.load)
        return vm
    }

    private fun ids(vm: FavoritesViewModel): List<String> =
        (vm.uiState.value.load as FavoritesLoad.Loaded).experts.map { it.expertId }

    @Test
    fun removeDropsTheRowAtOnceAndCallsDelete() =
        runTest(main.testDispatcher) {
            script(rows("a", "b", "c"))
            val vm = loadedViewModel("a", "b", "c")
            val gate = CompletableDeferred<ToggleFavoriteData>()
            api.removeFavoriteHandler = { gate.await() }

            vm.remove("b")
            runCurrent()

            assertEquals(listOf("a", "c"), ids(vm))
            assertEquals(listOf("b"), api.removeFavoriteRequests)
        }

    @Test
    fun removeFailureReinsertsAtTheOriginalIndex() =
        runTest(main.testDispatcher) {
            script(rows("a", "b", "c"))
            val vm = loadedViewModel("a", "b", "c")
            api.removeFavoriteHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"Expert not found."}""")
            }

            vm.remove("b")
            runCurrent()

            assertEquals(listOf("a", "b", "c"), ids(vm))
            assertEquals(FieldError.Text("Expert not found."), vm.uiState.value.removeFailed)
            vm.consumeRemoveFailure()
            assertEquals(null, vm.uiState.value.removeFailed)

            api.removeFavoriteHandler = { throw IOException() }
            vm.remove("b")
            runCurrent()
            assertEquals(listOf("a", "b", "c"), ids(vm))
            assertEquals(FieldError.Res(R.string.favorites_update_failed), vm.uiState.value.removeFailed)
        }

    @Test
    fun removingTheLastRowLeavesAnEmptyList() =
        runTest(main.testDispatcher) {
            script(rows("a"))
            val vm = loadedViewModel("a")
            api.removeFavoriteHandler = { toggleFavorite("a", isFavorite = false) }

            vm.remove("a")
            runCurrent()

            assertEquals(FavoritesLoad.Loaded(emptyList()), vm.uiState.value.load)
        }

    @Test
    fun reloadDuringRemovalDoesNotResurrect() =
        runTest(main.testDispatcher) {
            script(rows("a", "b"), rows("a", "b"))
            val vm = loadedViewModel("a", "b")
            val gate = CompletableDeferred<ToggleFavoriteData>()
            api.removeFavoriteHandler = { gate.await() }
            vm.remove("b")
            runCurrent()

            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()

            assertEquals(listOf("a"), ids(vm))
        }

    @Test
    fun staleReloadAfterSuccessfulDeleteDoesNotResurrect() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<FavoritesPayload>()
            script(rows("a", "b"), { gate.await() })
            val vm = loadedViewModel("a", "b")
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            api.removeFavoriteHandler = { toggleFavorite("b", isFavorite = false) }

            vm.remove("b")
            runCurrent()
            assertEquals(listOf("a"), ids(vm))

            gate.complete(favoritesPayload(favoriteItem("a"), favoriteItem("b")))
            runCurrent()

            assertEquals(listOf("a"), ids(vm))
        }

    @Test
    fun twoRemovalsBothFailingRestoreOrderWithoutDuplicates() =
        runTest(main.testDispatcher) {
            script(rows("a", "b", "c"))
            val vm = loadedViewModel("a", "b", "c")
            val gateA = CompletableDeferred<ToggleFavoriteData>()
            val gateC = CompletableDeferred<ToggleFavoriteData>()
            api.removeFavoriteHandler = { if (it == "a") gateA.await() else gateC.await() }

            vm.remove("a")
            vm.remove("c")
            runCurrent()
            assertEquals(listOf("b"), ids(vm))

            gateA.completeExceptionally(IOException())
            gateC.completeExceptionally(IOException())
            runCurrent()

            val restored = ids(vm)
            assertEquals(listOf("a", "b", "c"), restored.sorted())
            assertEquals(3, restored.size)
        }

    @Test
    fun removeOfAnUnknownIdDoesNothing() =
        runTest(main.testDispatcher) {
            script(rows("a"))
            val vm = loadedViewModel("a")

            vm.remove("zzz")
            runCurrent()

            assertEquals(0, api.removeFavoriteCalls)
            assertEquals(ui("a"), vm.uiState.value.load)
        }

    @Test
    fun viewHistoryPostsThePastLaunch() =
        runTest(main.testDispatcher) {
            val vm = viewModel()

            vm.viewHistory()

            assertEquals(BookingsLaunch(BookingsTab.Past), launchRequest.pending.value)
        }
}
