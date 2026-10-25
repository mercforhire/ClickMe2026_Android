@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertiseTagsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ExpertiseEditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val ok = SuccessMessageResponse(status = "success")

    private val audit = ExpertiseTagItem("T3", "Audit")
    private val law = ExpertiseTagItem("T2", "Law")
    private val tax = ExpertiseTagItem("T1", "Tax")
    private val catalog = listOf(tax, law, audit)

    init {
        api.expertiseTagsHandler = { ExpertiseTagsPayload(catalog) }
        api.expertProfileHandler = {
            ExpertProfileData(
                setupCompleted = true,
                expertiseTags = listOf(ExpertTag("T2", "Law"), ExpertTag("T1", "Tax", isPrimary = true)),
            )
        }
        api.updateExpertProfileTagsHandler = { ok }
    }

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        ExpertiseEditorViewModel(ExpertiseTagsRepository(api), saved)

    @Test
    fun seedsTheCatalogSortedAndTheSelectionPrimaryFirst() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            assertEquals(ExpertiseCatalogState.Loading, vm.uiState.value.catalog)

            advanceUntilIdle()

            assertEquals(ExpertiseCatalogState.Loaded(listOf(audit, law, tax)), vm.uiState.value.catalog)
            assertEquals(listOf(tax, law), vm.uiState.value.selected)
            assertEquals(1, api.expertProfileCalls)
            assertEquals(1, api.expertiseTagsCalls)
        }

    @Test
    fun queryFiltersRowsIgnoringCaseAndKeepsSelectedTagsListed() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onQueryChange("ta")

            assertEquals(listOf(tax), vm.uiState.value.rows)
            assertEquals(listOf(tax, law), vm.uiState.value.selected)

            vm.onQueryChange("zz")
            assertEquals(emptyList<ExpertiseTagItem>(), vm.uiState.value.rows)
        }

    @Test
    fun toggleAddsAtTheEndAndRemovesAndTheNextTagBecomesPrimary() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggle(audit)
            assertEquals(listOf(tax, law, audit), vm.uiState.value.selected)

            vm.toggle(tax)
            assertEquals(listOf(law, audit), vm.uiState.value.selected)

            vm.toggle(law)
            vm.toggle(audit)
            assertEquals(emptyList<ExpertiseTagItem>(), vm.uiState.value.selected)

            vm.toggle(law)
            assertEquals(listOf(law), vm.uiState.value.selected)
        }

    @Test
    fun toggleMatchesIdsIgnoringCase() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggle(ExpertiseTagItem("t1", "Tax"))

            assertEquals(listOf(law), vm.uiState.value.selected)
        }

    @Test
    fun saveWithAnEmptySelectionSendsNothing() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.remove("T1")
            vm.remove("T2")
            assertFalse(vm.uiState.value.canSave)

            vm.save()
            advanceUntilIdle()

            assertEquals(0, api.updateExpertProfileTagsCalls)
            assertFalse(vm.uiState.value.isSaving)
        }

    @Test
    fun saveSendsLowerCasedIdsWithTheFirstPrimaryAndIsSingleFlight() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.updateExpertProfileTagsHandler = {
                gate.await()
                ok
            }
            val vm = viewModel()
            advanceUntilIdle()

            vm.save()
            runCurrent()
            assertTrue(vm.uiState.value.isSaving)
            assertFalse(vm.uiState.value.canSave)
            vm.save()
            runCurrent()
            assertEquals(1, api.updateExpertProfileTagsCalls)

            gate.complete(Unit)
            advanceUntilIdle()

            val sent = api.updateExpertProfileTagsRequests.single().expertiseTags
            assertEquals(listOf("t1", "t2"), sent.map { it.tagId })
            assertEquals(listOf(true, false), sent.map { it.isPrimary })
            assertFalse(vm.uiState.value.isSaving)
            assertEquals(listOf(tax, law), vm.uiState.value.saved)

            vm.onSavedConsumed()
            assertNull(vm.uiState.value.saved)
        }

    @Test
    fun aSaveFailureShowsTheFirstFieldMessageAndKeepsTheSelection() =
        runTest(main.testDispatcher) {
            api.updateExpertProfileTagsHandler = {
                throw api.httpError(
                    422,
                    """{"message":"Validation failed","errors":[{"field":"expertise_tags","message":"Unknown tag"}]}""",
                )
            }
            val vm = viewModel()
            advanceUntilIdle()

            vm.save()
            advanceUntilIdle()

            assertEquals(FieldError.Text("Unknown tag"), vm.uiState.value.dialog)
            assertFalse(vm.uiState.value.isSaving)
            assertNull(vm.uiState.value.saved)
            assertEquals(listOf(tax, law), vm.uiState.value.selected)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun aCatalogFailureGivesFailedAndRetryReloads() =
        runTest(main.testDispatcher) {
            api.expertiseTagsHandler = { throw IOException("offline") }
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(
                ExpertiseCatalogState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.catalog,
            )

            api.expertiseTagsHandler = { ExpertiseTagsPayload(catalog) }
            vm.retry()
            advanceUntilIdle()

            assertEquals(ExpertiseCatalogState.Loaded(listOf(audit, law, tax)), vm.uiState.value.catalog)
            assertEquals(2, api.expertiseTagsCalls)
        }

    @Test
    fun aProfileLoadFailureLeavesTheSelectionEmptyAndTheCatalogUsable() =
        runTest(main.testDispatcher) {
            api.expertProfileHandler = { throw IOException("offline") }
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(emptyList<ExpertiseTagItem>(), vm.uiState.value.selected)
            assertEquals(ExpertiseCatalogState.Loaded(listOf(audit, law, tax)), vm.uiState.value.catalog)

            vm.toggle(law)
            assertEquals(listOf(law), vm.uiState.value.selected)
        }

    @Test
    fun savedStateRestoresTheSelectionAndQueryInsteadOfSeeding() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            val first = viewModel(saved)
            advanceUntilIdle()
            first.toggle(audit)
            first.onQueryChange("au")

            val restored = viewModel(saved)
            advanceUntilIdle()

            assertEquals(listOf(tax, law, audit), restored.uiState.value.selected)
            assertEquals("au", restored.uiState.value.query)
            assertEquals(1, api.expertProfileCalls)
        }
}
