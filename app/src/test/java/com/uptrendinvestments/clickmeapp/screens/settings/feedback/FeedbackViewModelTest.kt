@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.feedbackSubmission
import com.uptrendinvestments.clickmeapp.testing.feedbackTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class FeedbackViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.harness() =
        SignupTestHarness(this, tmp, main.testDispatcher).also {
            it.api.feedbackTypesHandler = { feedbackTypes() }
            it.api.submitFeedbackHandler = { feedbackSubmission() }
        }

    private fun SignupTestHarness.vm() = FeedbackViewModel(FeedbackRepository(api), userManager)

    @Test
    fun loadsTypesAndPreselectsTheFirst() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = h.vm()
            assertEquals(FeedbackLoad.Loading, vm.uiState.value.load)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(FeedbackLoad.Ready, state.load)
            assertEquals("bug_report", state.selectedTypeId)
            assertEquals(
                listOf("bug_report", "feature_request", "general", "billing", "content"),
                state.types.map { it.id },
            )
        }

    @Test
    fun aValidFormSendsTrimmedTextAndShowsThanks() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = h.vm()
            runCurrent()
            vm.onDetailsChange("  0123456789  ")
            vm.onEmailChange("  a@b.co  ")

            vm.submit()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            runCurrent()

            assertEquals(
                listOf(SubmitFeedbackRequest("bug_report", "0123456789", "a@b.co")),
                h.api.submitFeedbackRequests,
            )
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertTrue(vm.uiState.value.showThanks)
        }

    @Test
    fun typesFailureIsTheErrorStateAndRetryLoads() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.feedbackTypesHandler = { throw IOException("offline") }
            val vm = h.vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is FeedbackLoad.Failed)

            h.api.feedbackTypesHandler = { feedbackTypes() }
            vm.retry()
            runCurrent()
            assertEquals(FeedbackLoad.Ready, vm.uiState.value.load)
        }

    @Test
    fun anEmptyTypeListIsTheErrorState() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.feedbackTypesHandler = { feedbackTypes().copy(types = emptyList()) }
            val vm = h.vm()
            runCurrent()

            val load = vm.uiState.value.load
            assertTrue(load is FeedbackLoad.Failed)
            assertTrue((load as FeedbackLoad.Failed).message is FieldError.Res)
        }
}
