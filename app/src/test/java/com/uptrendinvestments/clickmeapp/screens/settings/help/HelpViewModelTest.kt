package com.uptrendinvestments.clickmeapp.screens.settings.help

import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
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
}
