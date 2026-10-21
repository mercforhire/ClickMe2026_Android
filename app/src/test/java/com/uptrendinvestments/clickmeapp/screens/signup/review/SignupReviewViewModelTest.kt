@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.review

import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.screens.signup.SignupDraft
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CompletableDeferred
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

class SignupReviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.setupProfileHandler = { SetupExpertProfileData() }
            it.api.emailStatusHandler = { EmailStatusData(verified = false) }
        }

    private suspend fun SignupTestHarness.hydrateFull(
        province: String? = "ON",
        avatarUrl: String? = "https://cdn.example/avatar.jpg",
    ) {
        api.expertProfileHandler = { fullProfile(province = province, avatarUrl = avatarUrl) }
        repository.hydrate()
    }

    @Test
    fun freshDraftShowsEverythingRequiredExceptTheDeviceTimezone() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = SignupReviewViewModel(h.repository)

            val s = vm.uiState.value
            assertNull(s.firstName)
            assertNull(s.location)
            assertNull(s.languages)
            assertNull(s.avatarUrl)
            assertNull(s.skills)
            assertEquals(Timezones.entryFor(Timezones.detected().id).label, s.timezoneLabel)
            assertEquals(listOf(SetupStep.PersonalDetails, SetupStep.ProfilePhoto, SetupStep.Expertise), s.missing)
            assertFalse(s.canPublish)
        }

    @Test
    fun fullDraftShowsValuesAndCanPublish() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)

            val s = vm.uiState.value
            assertEquals("Ada", s.firstName)
            assertEquals("Toronto, ON, Canada", s.location)
            assertEquals("English", s.languages)
            assertEquals(Timezones.entryFor("America/Toronto").label, s.timezoneLabel)
            assertEquals("Tax", s.skills)
            assertTrue(s.missing.isEmpty())
            assertTrue(s.canPublish)
        }

    @Test
    fun emptyProvinceIsOmittedFromTheLocation() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull(province = null)
            val vm = SignupReviewViewModel(h.repository)

            assertEquals("Toronto, Canada", vm.uiState.value.location)
            assertTrue(vm.uiState.value.canPublish)
        }

    @Test
    fun customLanguagesAreListedAfterCatalogueLanguages() {
        val s = SignupDraft(languages = listOf(lang("fr", "French")), customLanguages = listOf("Klingon")).toUiState()
        assertEquals("French, Klingon", s.languages)
        assertEquals("Klingon", SignupDraft(customLanguages = listOf("Klingon")).toUiState().languages)
    }

    @Test
    fun skillsAndLanguagesKeepDraftOrder() {
        val draft =
            SignupDraft(
                languages = listOf(lang("fr", "French"), lang("en", "English")),
                expertiseTags = listOf(tag("b", "Audit"), tag("a", "Tax")),
            )
        val s = draft.toUiState()
        assertEquals("French, English", s.languages)
        assertEquals("Audit, Tax", s.skills)
    }

    @Test
    fun unverifiedEmailDoesNotBlockPublish() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)

            assertFalse(h.repository.draft.value.emailVerified)
            assertTrue(vm.uiState.value.canPublish)
            assertFalse(
                vm.uiState.value.missing
                    .contains(SetupStep.VerifyEmail),
            )
        }

    @Test
    fun publishWhenNotAllowedMakesNoCall() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = SignupReviewViewModel(h.repository)

            vm.publish()
            advanceUntilIdle()

            assertTrue(h.api.setupProfileRequests.isEmpty())
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertFalse(vm.published.value)
        }

    @Test
    fun publishSendsOnceHoldsThenResetsTheRepositoryAndSignalsPublished() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            h.api.setupProfileHandler = {
                gate.await()
                SetupExpertProfileData()
            }

            vm.publish()
            runCurrent()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            // A second publish while Saving makes no second call.
            vm.publish()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(1, h.api.setupProfileRequests.size)
            assertEquals(
                true,
                h.api.setupProfileRequests
                    .single()
                    .setupCompleted,
            )
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertFalse(vm.published.value)
            // Not reset yet: the hold is still running.
            assertEquals("Ada", h.repository.draft.value.firstName)

            advanceTimeBy(599)
            runCurrent()
            assertFalse(vm.published.value)
            advanceTimeBy(2)
            runCurrent()

            assertTrue(vm.published.value)
            // The repository was reset (T-04-46) ...
            assertEquals("", h.repository.draft.value.firstName)
            assertNull(h.repository.draft.value.avatarUrl)
            // ... but the screen keeps showing the published values.
            val s = vm.uiState.value
            assertEquals("Ada", s.firstName)
            assertEquals("Toronto, ON, Canada", s.location)
            assertEquals("Tax", s.skills)
            assertEquals(SaveState.Saved, s.saveState)

            vm.onPublishedConsumed()
            assertFalse(vm.published.value)
        }

    @Test
    fun missingFieldsFrom422MapToHubStepsInOrderWithoutDuplicates() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)
            runCurrent()
            h.api.setupProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"message":"incomplete","missing_fields":["profile_image_url","languages","first_name"]}""",
                )
            }

            vm.publish()
            advanceUntilIdle()

            val s = vm.uiState.value
            assertEquals(listOf(SetupStep.PersonalDetails, SetupStep.ProfilePhoto), s.missingFieldsDialog)
            assertEquals(SaveState.Idle, s.saveState)
            assertNull(s.dialogError)
            assertFalse(vm.published.value)
            // The repository marked the steps blocked, so Publish is disabled again.
            assertFalse(s.canPublish)
            assertFalse(vm.returnToHub.value)

            vm.dismissMissingFields()
            assertNull(vm.uiState.value.missingFieldsDialog)
            assertTrue(vm.returnToHub.value)
            vm.onReturnToHubConsumed()
            assertFalse(vm.returnToHub.value)
        }

    @Test
    fun serverErrorShowsADialogAndReturnsToIdle() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)
            runCurrent()
            h.api.setupProfileHandler = { throw h.api.httpError(500, """{"message":"boom"}""") }

            vm.publish()
            advanceUntilIdle()

            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertNotNull(vm.uiState.value.dialogError)
            assertNull(vm.uiState.value.missingFieldsDialog)
            assertEquals("Ada", h.repository.draft.value.firstName)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun networkFailureShowsADialogAndReturnsToIdle() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.hydrateFull()
            val vm = SignupReviewViewModel(h.repository)
            runCurrent()
            h.api.setupProfileHandler = { throw IOException("offline") }

            vm.publish()
            advanceUntilIdle()

            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertNotNull(vm.uiState.value.dialogError)
            assertFalse(vm.published.value)
        }

    @Test
    fun stateFollowsDraftEditsBeforePublishing() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = SignupReviewViewModel(h.repository)
            runCurrent()
            assertFalse(vm.uiState.value.canPublish)

            h.hydrateFull()
            runCurrent()

            assertEquals("Ada", vm.uiState.value.firstName)
            assertTrue(vm.uiState.value.canPublish)
        }

    private fun lang(
        id: String,
        label: String,
    ) = com.uptrendinvestments.clickmeapp.model.responses
        .LanguageItem(id, label)

    private fun tag(
        id: String,
        label: String,
    ) = com.uptrendinvestments.clickmeapp.model.responses
        .ExpertiseTagItem(id, label)
}
