@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.tags

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SignupTagsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val tax = ExpertiseTagItem("t1", "Tax")
    private val law = ExpertiseTagItem("t2", "Law")
    private val audit = ExpertiseTagItem("t3", "Audit")
    private val catalog = listOf(tax, law, audit)

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.setupProfileHandler = { SetupExpertProfileData() }
            it.api.expertiseTagsHandler = { ExpertiseTagsPayload(catalog) }
        }

    private fun viewModel(
        h: SignupTestHarness,
        saved: SavedStateHandle = SavedStateHandle(),
    ) = SignupTagsViewModel(h.repository, h.api, saved)

    private suspend fun hydrateWithTags(
        h: SignupTestHarness,
        vararg tags: ExpertTag,
    ) {
        h.api.expertProfileHandler = { h.fullProfile(tags = tags.toList()) }
        h.api.emailStatusHandler = { EmailStatusData(verified = false) }
        h.repository.hydrate()
    }

    @Test
    fun loadsTheCatalogOnceAndExposesItAsLoaded() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            assertEquals(TagCatalog.Loading, vm.uiState.value.catalog)

            advanceUntilIdle()

            assertEquals(TagCatalog.Loaded(catalog), vm.uiState.value.catalog)
            assertEquals(1, h.api.expertiseTagsCalls)
            assertEquals(catalog, vm.uiState.value.suggested)
        }

    @Test
    fun aFailedLoadGivesFailedAndRetryReloads() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertiseTagsHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            advanceUntilIdle()
            assertEquals(TagCatalog.Failed, vm.uiState.value.catalog)

            h.api.expertiseTagsHandler = { ExpertiseTagsPayload(catalog) }
            vm.retry()
            advanceUntilIdle()

            assertEquals(TagCatalog.Loaded(catalog), vm.uiState.value.catalog)
            assertEquals(2, h.api.expertiseTagsCalls)
        }

    @Test
    fun selectionKeepsTapOrderAndLeavesTheSuggestedList() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))
            advanceUntilIdle()

            vm.select(tax)
            vm.select(law)
            vm.select(tax)

            assertEquals(listOf(tax, law), vm.uiState.value.selected)
            assertEquals(listOf(audit), vm.uiState.value.suggested)

            vm.remove("t1")

            assertEquals(listOf(law), vm.uiState.value.selected)
            assertEquals(listOf(tax, audit), vm.uiState.value.suggested)
        }

    @Test
    fun queryFiltersSuggestedIgnoringCaseAndFlagsNoMatch() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))
            advanceUntilIdle()

            vm.onQueryChange("TA")
            assertEquals(listOf(tax), vm.uiState.value.suggested)
            assertFalse(vm.uiState.value.noMatch)

            vm.onQueryChange("zz")
            assertTrue(vm.uiState.value.suggested.isEmpty())
            assertTrue(vm.uiState.value.noMatch)

            vm.onQueryChange("")
            assertFalse(vm.uiState.value.noMatch)
        }

    @Test
    fun saveSendsTheIdsInTapOrderThenHoldsSavedFor600ms() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            advanceUntilIdle()
            vm.select(law)
            vm.select(tax)

            vm.save()
            runCurrent()

            assertEquals(listOf("t2", "t1"), h.api.setupProfileRequests.single().expertiseTags)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertFalse(vm.saved.value)

            advanceTimeBy(599)
            assertFalse(vm.saved.value)
            advanceTimeBy(2)
            assertTrue(vm.saved.value)

            vm.onSavedConsumed()
            assertFalse(vm.saved.value)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun emptyingAPreviouslySavedListSendsAnEmptyArray() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateWithTags(h, ExpertTag("t1", "Tax"))
            val vm = viewModel(h)
            advanceUntilIdle()
            assertEquals(listOf(tax), vm.uiState.value.selected)

            vm.remove("t1")
            vm.save()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), h.api.setupProfileRequests.single().expertiseTags)
            assertTrue(vm.saved.value)
        }

    @Test
    fun savingWithNoTagsAndAnEmptyDraftSendsNothingButStillCompletes() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            advanceUntilIdle()

            vm.save()
            advanceUntilIdle()

            assertEquals(0, h.api.setupProfileCalls)
            assertTrue(vm.saved.value)
        }

    @Test
    fun aSaveFailureShowsTheDialogAndReturnsToIdleKeepingTheSelection() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            advanceUntilIdle()
            vm.select(tax)

            vm.save()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertEquals(listOf(tax), vm.uiState.value.selected)
            assertFalse(vm.saved.value)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun selectionAndQueryAreRestoredFromSavedState() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val saved = SavedStateHandle()
            val vm = viewModel(h, saved)
            advanceUntilIdle()
            vm.select(law)
            vm.select(tax)
            vm.onQueryChange("au")

            val restored = viewModel(h, saved)
            advanceUntilIdle()

            assertEquals(listOf(law, tax), restored.uiState.value.selected)
            assertEquals("au", restored.uiState.value.query)
        }

    @Test
    fun aLateHydrateReseedsAnUntouchedFormButNotAnEditedOne() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val untouched = viewModel(h)
            val edited = viewModel(h)
            advanceUntilIdle()
            edited.select(audit)
            assertTrue(untouched.uiState.value.selected.isEmpty())

            hydrateWithTags(h, ExpertTag("t1", "Tax"), ExpertTag("t2", "Law"))
            advanceUntilIdle()

            assertEquals(listOf(tax, law), untouched.uiState.value.selected)
            assertEquals(listOf(audit), edited.uiState.value.selected)
        }
}
