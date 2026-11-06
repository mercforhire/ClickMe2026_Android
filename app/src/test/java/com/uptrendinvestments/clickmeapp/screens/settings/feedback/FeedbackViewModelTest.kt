@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiErrorParser
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

    private fun SignupTestHarness.vm(handle: SavedStateHandle = SavedStateHandle()) =
        FeedbackViewModel(FeedbackRepository(api), userManager, handle)

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

    /** Runs a valid submit whose request fails with [failure], and returns the state after it. */
    private fun TestScope.failedSend(failure: Exception): FeedbackUiState {
        val h = harness()
        h.api.submitFeedbackHandler = { throw failure }
        val vm = readyVm(h, "0123456789")
        vm.onEmailChange("a@b.co")
        vm.onSelectType("billing")
        vm.submit()
        runCurrent()
        return vm.uiState.value
    }

    private fun http(
        status: Int,
        body: String,
    ) = ApiErrorParser.httpError(status, body)

    private fun validation(
        field: String,
        message: String,
    ) = http(
        422,
        """{"status":"error","code":"VALIDATION_ERROR","message":"Validation failed",""" +
            """"errors":[{"field":"$field","message":"$message"}]}""",
    )

    @Test
    fun validation422OnDetailsShowsTheInlineError() =
        runTest(main.testDispatcher) {
            val state = failedSend(validation("details", "Too short"))

            assertTrue(state.showDetailsError)
            assertEquals(null, state.dialog)
            assertEquals(SaveState.Idle, state.saveState)
            assertEquals("0123456789", state.details)
        }

    @Test
    fun validation422OnEmailShowsOnTheEmailField() =
        runTest(main.testDispatcher) {
            val state = failedSend(validation("email", "Invalid email"))

            assertEquals("Invalid email", state.emailError)
            assertEquals(null, state.dialog)
            assertEquals("a@b.co", state.email)
        }

    @Test
    fun validation422OnTheTypeShowsTheServerMessage() =
        runTest(main.testDispatcher) {
            val state = failedSend(validation("type_id", "Unknown type"))

            assertEquals(FieldError.Text("Unknown type"), state.dialog)
            assertFalse(state.showDetailsError)
        }

    @Test
    fun validation422WithAnUnnamedFieldPrefersItsMessage() =
        runTest(main.testDispatcher) {
            val state = failedSend(validation("other", "Bad"))

            assertEquals(FieldError.Text("Bad"), state.dialog)
        }

    @Test
    fun status429ShowsTheFixedCopy() =
        runTest(main.testDispatcher) {
            val state =
                failedSend(http(429, """{"status":"error","code":"TOO_MANY_REQUESTS","message":"Slow down"}"""))

            assertEquals(FieldError.Res(R.string.feedback_too_many_attempts), state.dialog)
        }

    @Test
    fun otherHttpShowsTheServerMessage() =
        runTest(main.testDispatcher) {
            val withMessage = failedSend(http(500, """{"status":"error","message":"Boom"}"""))
            assertEquals(FieldError.Text("Boom"), withMessage.dialog)

            val bare = failedSend(http(500, "{}"))
            assertEquals(FieldError.Res(R.string.feedback_send_failed), bare.dialog)
        }

    @Test
    fun networkFailureShowsTheConnectionCopy() =
        runTest(main.testDispatcher) {
            val state = failedSend(IOException("offline"))

            assertEquals(FieldError.Res(R.string.login_error_network), state.dialog)
        }

    @Test
    fun suspendedShowsNothingHere() =
        runTest(main.testDispatcher) {
            val state =
                failedSend(
                    http(
                        403,
                        """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Your account is suspended"}""",
                    ),
                )

            assertEquals(null, state.dialog)
            assertEquals(SaveState.Idle, state.saveState)
        }

    @Test
    fun theFormIsKeptOnEveryFailure() =
        runTest(main.testDispatcher) {
            val failures =
                listOf(
                    validation("details", "Too short"),
                    validation("email", "Invalid email"),
                    validation("type_id", "Unknown type"),
                    http(429, "{}"),
                    http(500, "{}"),
                    IOException("offline"),
                )
            failures.forEach { failure ->
                val state = failedSend(failure)

                assertEquals("0123456789", state.details)
                assertEquals("a@b.co", state.email)
                assertEquals("billing", state.selectedTypeId)
                assertFalse(state.showThanks)
            }
        }

    @Test
    fun dismissDialogClearsIt() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.submitFeedbackHandler = { throw http(500, """{"status":"error","message":"Boom"}""") }
            val vm = readyVm(h, "0123456789")
            vm.submit()
            runCurrent()
            assertEquals(FieldError.Text("Boom"), vm.uiState.value.dialog)

            vm.dismissDialog()

            assertEquals(null, vm.uiState.value.dialog)
        }

    @Test
    fun aFailedSendCanBeRetried() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.submitFeedbackHandler = { throw IOException("offline") }
            val vm = readyVm(h, "0123456789")
            vm.submit()
            runCurrent()

            h.api.submitFeedbackHandler = { feedbackSubmission() }
            vm.dismissDialog()
            vm.submit()
            runCurrent()

            assertEquals(2, h.api.submitFeedbackCalls)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
        }

    // Process death (Phase 17 D-09, POL-02)

    private val feedbackKeys = listOf("feedback_type", "feedback_details", "feedback_email", "feedback_email_edited")

    @Test
    fun restoresTheFormAfterProcessDeath() =
        runTest(main.testDispatcher) {
            val h = harness()
            val handle =
                SavedStateHandle(
                    mapOf(
                        "feedback_type" to "feature_request",
                        "feedback_details" to "Ten chars plus",
                        "feedback_email" to "me@x.com",
                        "feedback_email_edited" to true,
                    ),
                )
            val vm = h.vm(handle)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(FeedbackLoad.Ready, state.load)
            assertEquals("feature_request", state.selectedTypeId)
            assertEquals("Ten chars plus", state.details)
            assertEquals("me@x.com", state.email)
        }

    @Test
    fun signedInPrefillIsNotWrittenToSavedState() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.knowMe("me@example.com")
            val handle = SavedStateHandle()
            val vm = h.vm(handle)
            runCurrent()

            assertEquals("me@example.com", vm.uiState.value.email)
            assertFalse(handle.contains("feedback_email"))
            assertFalse(handle.contains("feedback_email_edited"))

            vm.onEmailChange("typed@example.com")
            assertEquals("typed@example.com", handle.get<String>("feedback_email"))
            assertEquals(true, handle.get<Boolean>("feedback_email_edited"))
        }

    @Test
    fun aClearedEmailStaysClearedAfterProcessDeath() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.knowMe("me@example.com")
            val vm = h.vm(SavedStateHandle(mapOf("feedback_email" to "", "feedback_email_edited" to true)))
            runCurrent()

            assertEquals("", vm.uiState.value.email)
        }

    @Test
    fun staleRestoredTypeFallsBackToTheFirstType() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = h.vm(SavedStateHandle(mapOf("feedback_type" to "retired_type")))
            runCurrent()

            assertEquals("bug_report", vm.uiState.value.selectedTypeId)
        }

    @Test
    fun editsAreMirroredToSavedState() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = harness().vm(handle)
            runCurrent()
            vm.onSelectType("billing")
            vm.onDetailsChange("some details")

            assertEquals("billing", handle.get<String>("feedback_type"))
            assertEquals("some details", handle.get<String>("feedback_details"))
        }

    @Test
    fun savedFormIsClearedAfterASuccessfulSend() =
        runTest(main.testDispatcher) {
            val h = harness()
            val handle = SavedStateHandle()
            val vm = h.vm(handle)
            runCurrent()
            vm.onSelectType("billing")
            vm.onDetailsChange("0123456789")
            vm.onEmailChange("a@b.co")
            feedbackKeys.forEach { assertTrue(it, handle.contains(it)) }

            vm.submit()
            runCurrent()

            assertTrue(vm.uiState.value.showThanks)
            feedbackKeys.forEach { assertFalse(it, handle.contains(it)) }
        }

    @Test
    fun toStringStaysRedactedWithRestoredText() =
        runTest(main.testDispatcher) {
            val vm =
                harness().vm(
                    SavedStateHandle(mapOf("feedback_details" to "secret-details", "feedback_email" to "secret@x.com")),
                )
            runCurrent()

            assertEquals("secret-details", vm.uiState.value.details)
            assertFalse(vm.uiState.value.toString().contains("secret"))
        }
}
