package com.uptrendinvestments.clickmeapp.screens.settings.help

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.faqArticle
import com.uptrendinvestments.clickmeapp.testing.faqCategory
import com.uptrendinvestments.clickmeapp.testing.faqsPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val articles = listOf(faqArticle())

    private fun vm() = HelpViewModel(HelpRepository(api))

    private fun HelpViewModel.titles() = (uiState.value.load as HelpLoad.Loaded).categories.map { it.title }

    @Test
    fun sectionsFilterEmptyAndSortByOrder() {
        val payload =
            faqsPayload(
                faqCategory(id = "c1", title = "Payments", order = 2, articles = articles),
                faqCategory(id = "c2", title = "Account", order = 1, articles = articles),
                faqCategory(id = "c3", title = "Empty", order = 3),
                faqCategory(id = "c4", title = "Calls", order = 1, articles = articles),
            )

        assertEquals(listOf("Account", "Calls", "Payments"), buildHelpSections(payload).map { it.title })
    }

    @Test
    fun allEmptyCategoriesGiveNoSections() {
        assertEquals(emptyList<Any>(), buildHelpSections(FaqsPayload(categories = listOf(faqCategory()))))
    }

    @Test
    fun firstLoadIsLoadingThenLoaded() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.faqsHandler = {
                gate.await()
                faqsPayload(faqCategory(title = "Account", articles = articles))
            }
            val vm = vm()
            runCurrent()
            assertEquals(HelpLoad.Loading, vm.uiState.value.load)

            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf("Account"), vm.titles())
            assertEquals(1, api.faqsCalls)
        }

    @Test
    fun failureShowsServerMessageOrNetworkCopyAndRetryReloads() =
        runTest(main.testDispatcher) {
            api.faqsHandler = { throw api.httpError(500, """{"status":"error","message":"Down"}""") }
            val vm = vm()
            runCurrent()
            assertEquals(HelpLoad.Failed(FieldError.Text("Down")), vm.uiState.value.load)

            api.faqsHandler = { throw IOException("offline") }
            vm.retry()
            runCurrent()
            assertEquals(HelpLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)

            api.faqsHandler = { faqsPayload(faqCategory(title = "Account", articles = articles)) }
            vm.retry()
            runCurrent()
            assertEquals(listOf("Account"), vm.titles())
            assertEquals(3, api.faqsCalls)
        }

    @Test
    fun retryWhileLoadingKeepsOnlyTheNewestAnswer() =
        runTest(main.testDispatcher) {
            val first = CompletableDeferred<Unit>()
            api.faqsHandler = {
                first.await()
                faqsPayload(faqCategory(title = "Old", articles = articles))
            }
            val vm = vm()
            runCurrent()

            api.faqsHandler = { faqsPayload(faqCategory(title = "New", articles = articles)) }
            vm.retry()
            runCurrent()
            first.complete(Unit)
            runCurrent()

            assertEquals(listOf("New"), vm.titles())
        }

    @Test
    fun refreshReplacesTheListQuietly() =
        runTest(main.testDispatcher) {
            api.faqsHandler = { faqsPayload(faqCategory(title = "Account", articles = articles)) }
            val vm = vm()
            runCurrent()

            api.faqsHandler = { faqsPayload(faqCategory(title = "Calls", articles = articles)) }
            vm.refresh()
            runCurrent()

            assertEquals(listOf("Calls"), vm.titles())
            assertEquals(false, vm.uiState.value.isRefreshing)
            assertEquals(false, vm.uiState.value.refreshFailed)
        }

    @Test
    fun refreshFailureKeepsListAndFlags() =
        runTest(main.testDispatcher) {
            api.faqsHandler = { faqsPayload(faqCategory(title = "Account", articles = articles)) }
            val vm = vm()
            runCurrent()

            api.faqsHandler = { throw IOException("offline") }
            vm.refresh()
            runCurrent()

            assertEquals(listOf("Account"), vm.titles())
            assertEquals(true, vm.uiState.value.refreshFailed)
            assertEquals(false, vm.uiState.value.isRefreshing)

            vm.consumeRefreshFailure()
            assertEquals(false, vm.uiState.value.refreshFailed)
        }
}
