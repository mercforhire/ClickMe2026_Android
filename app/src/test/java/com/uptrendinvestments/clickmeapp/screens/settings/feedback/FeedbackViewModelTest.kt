@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.feedbackSubmission
import com.uptrendinvestments.clickmeapp.testing.feedbackTypes
import kotlinx.coroutines.CompletableDeferred
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

    private fun TestScope.readyVm(
        h: SignupTestHarness,
        details: String = "",
    ): FeedbackViewModel {
        val vm = h.vm()
        runCurrent()
        vm.onDetailsChange(details)
        return vm
    }

    @Test
    fun nineTrimmedCharactersSendNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "  012345678  ")

            vm.submit()
            runCurrent()

            assertTrue(vm.uiState.value.showDetailsError)
            assertEquals(0, h.api.submitFeedbackCalls)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun tenTrimmedCharactersSend() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "  0123456789  ")

            vm.submit()
            runCurrent()

            assertEquals(1, h.api.submitFeedbackCalls)
            assertEquals(
                "0123456789",
                h.api.submitFeedbackRequests
                    .single()
                    .details,
            )
            assertFalse(vm.uiState.value.showDetailsError)
        }

    @Test
    fun whitespaceOnlyCountsAsEmpty() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, " ".repeat(12))

            vm.submit()
            runCurrent()

            assertTrue(vm.uiState.value.showDetailsError)
            assertEquals(0, h.api.submitFeedbackCalls)
        }

    @Test
    fun theErrorClearsAtTen() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "012")
            vm.submit()
            assertTrue(vm.uiState.value.showDetailsError)

            vm.onDetailsChange("012345678")
            assertTrue(vm.uiState.value.showDetailsError)
            vm.onDetailsChange("0123456789")
            assertFalse(vm.uiState.value.showDetailsError)
            assertEquals(0, h.api.submitFeedbackCalls)
        }

    private suspend fun SignupTestHarness.knowMe(email: String) {
        api.meHandler = { MeData(id = "u1", email = email, roles = listOf("client")) }
        signIn()
        userManager.refreshMe()
    }

    @Test
    fun emailPrefillsFromMeWhenEmpty() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.knowMe("me@example.com")

            val vm = readyVm(h)

            assertEquals("me@example.com", vm.uiState.value.email)
        }

    @Test
    fun emailFallsBackToTheCachedProfile() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.userProfileHandler = {
                UserProfileData(
                    personalDetails = UserProfileData.PersonalDetails(email = "cached@example.com"),
                    professionalDetails = UserProfileData.ProfessionalDetails(),
                    location = UserProfileData.Location(),
                )
            }
            h.signIn()
            h.userManager.refreshProfile()
            assertEquals(null, h.userManager.me.value)

            val vm = readyVm(h)

            assertEquals("cached@example.com", vm.uiState.value.email)
        }

    @Test
    fun noKnownEmailLeavesItEmpty() =
        runTest(main.testDispatcher) {
            val vm = readyVm(harness())

            assertEquals("", vm.uiState.value.email)
        }

    @Test
    fun aTypedEmailIsNotOverwritten() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.knowMe("me@example.com")
            val vm = readyVm(h)
            vm.onEmailChange("typed@example.com")

            vm.retry()
            runCurrent()

            assertEquals("typed@example.com", vm.uiState.value.email)
        }

    @Test
    fun blankEmailIsOmitted() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "0123456789")
            vm.onEmailChange("   ")

            vm.submit()
            runCurrent()

            assertEquals(
                null,
                h.api.submitFeedbackRequests
                    .single()
                    .email,
            )
        }

    @Test
    fun theUserCanClearThePrefilledEmailAndSend() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.knowMe("me@example.com")
            val vm = readyVm(h, "0123456789")
            vm.onEmailChange("")

            vm.submit()
            runCurrent()

            assertEquals(
                null,
                h.api.submitFeedbackRequests
                    .single()
                    .email,
            )
        }

    @Test
    fun repeatedTapsAreIgnoredWhileSending() =
        runTest(main.testDispatcher) {
            val h = harness()
            val gate = CompletableDeferred<Unit>()
            h.api.submitFeedbackHandler = {
                gate.await()
                feedbackSubmission()
            }
            val vm = readyVm(h, "0123456789")

            vm.submit()
            runCurrent()
            vm.submit()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(1, h.api.submitFeedbackCalls)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
        }

    @Test
    fun repeatedTapsAreIgnoredAfterSuccess() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "0123456789")
            vm.submit()
            runCurrent()

            vm.submit()
            runCurrent()

            assertEquals(1, h.api.submitFeedbackCalls)
        }

    @Test
    fun selectingATypeUsesIt() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = readyVm(h, "0123456789")
            vm.onSelectType("billing")

            vm.submit()
            runCurrent()

            assertEquals(
                "billing",
                h.api.submitFeedbackRequests
                    .single()
                    .typeId,
            )
        }

    @Test
    fun noCategoryShowsTheDialog() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.feedbackTypesHandler = { throw IOException("offline") }
            val vm = readyVm(h, "0123456789")
            assertEquals(null, vm.uiState.value.selectedTypeId)

            vm.submit()
            runCurrent()

            assertEquals(FieldError.Res(R.string.feedback_no_category), vm.uiState.value.dialog)
            assertEquals(0, h.api.submitFeedbackCalls)
        }
}
