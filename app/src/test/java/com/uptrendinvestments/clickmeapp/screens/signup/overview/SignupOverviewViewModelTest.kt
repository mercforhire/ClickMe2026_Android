@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.overview

import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
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

class SignupOverviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.expertProfileHandler = { it.fullProfile() }
            it.api.emailStatusHandler = { EmailStatusData(verified = false) }
        }

    private fun viewModel(h: SignupTestHarness) = SignupOverviewViewModel(h.repository, h.userManager)

    private fun SignupOverviewUiState.completed(step: SetupStep) = rows.first { it.step == step }.completed

    @Test
    fun emptyDraftListsFiveStepsInOrderOnlyTheDefaultTimezoneComplete() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))
            runCurrent()

            val state = vm.uiState.value
            assertEquals(SetupStep.entries.toList(), state.rows.map { it.step })
            // The repository seeds the device timezone (plan 04-07), so Timezone starts checked.
            assertEquals(listOf(SetupStep.Timezone), state.rows.filter { it.completed }.map { it.step })
            assertEquals(1, state.completedCount)
        }

    @Test
    fun hydratedFullProfileChecksEverythingButUnverifiedEmail() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            h.repository.hydrate()
            runCurrent()

            val state = vm.uiState.value
            assertTrue(state.completed(SetupStep.PersonalDetails))
            assertTrue(state.completed(SetupStep.Timezone))
            assertTrue(state.completed(SetupStep.ProfilePhoto))
            assertTrue(state.completed(SetupStep.Expertise))
            assertFalse(state.completed(SetupStep.VerifyEmail))
            assertEquals(4, state.completedCount)
        }

    @Test
    fun isRefreshingMirrorsLoadingAndFailureCanBeAcknowledged() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)

            vm.refresh()
            runCurrent()

            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)

            vm.onRefreshErrorShown()
            runCurrent()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun aNewHydrateResetsTheFailureAcknowledgement() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            vm.refresh()
            runCurrent()
            vm.onRefreshErrorShown()
            runCurrent()
            assertFalse(vm.uiState.value.refreshFailed)

            vm.refresh()
            runCurrent()

            assertTrue(vm.uiState.value.refreshFailed)
        }

    @Test
    fun onShownWhenLoadedRefreshesEmailStatusOnceAndChecksVerifyEmail() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            h.repository.hydrate()
            runCurrent()
            val statusCallsAfterHydrate = h.api.emailStatusCalls
            h.api.emailStatusHandler = { EmailStatusData(verified = true) }

            vm.onShown()
            runCurrent()

            assertEquals(statusCallsAfterHydrate + 1, h.api.emailStatusCalls)
            assertTrue(vm.uiState.value.completed(SetupStep.VerifyEmail))
            assertEquals(5, vm.uiState.value.completedCount)
        }

    @Test
    fun onShownWhenFailedHydratesAgain() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            h.repository.hydrate()
            runCurrent()
            assertEquals(1, h.api.expertProfileCalls)

            vm.onShown()
            runCurrent()

            assertEquals(2, h.api.expertProfileCalls)
        }

    @Test
    fun onShownWhenIdleMakesNoCall() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            vm.onShown()
            runCurrent()

            assertEquals(0, h.api.expertProfileCalls)
            assertEquals(0, h.api.emailStatusCalls)
        }

    @Test
    fun failingEmailStatusRefreshOnShownIsSwallowed() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            h.repository.hydrate()
            runCurrent()
            val before = vm.uiState.value.rows
            h.api.emailStatusHandler = { throw IOException("offline") }

            vm.onShown()
            runCurrent()

            assertEquals(before, vm.uiState.value.rows)
        }

    @Test
    fun refreshHydratesProfileAndStatusOnce() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            vm.refresh()
            runCurrent()

            assertEquals(1, h.api.expertProfileCalls)
            assertEquals(1, h.api.emailStatusCalls)
        }

    @Test
    fun publishBlockedStepShowsIncomplete() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            h.repository.hydrate()
            h.api.setupProfileHandler = {
                throw h.api.httpError(422, """{"message":"incomplete","missing_fields":["profile_image_url"]}""")
            }
            try {
                h.repository.publish()
            } catch (e: ApiException) {
                // Expected: the 422 blocks the mapped step.
            }
            runCurrent()

            assertFalse(vm.uiState.value.completed(SetupStep.ProfilePhoto))
            assertTrue(vm.uiState.value.completed(SetupStep.Expertise))
        }

    @Test
    fun logoutConfirmationFlowAndConfirm() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.signIn()
            val vm = viewModel(h)
            runCurrent()
            assertTrue(h.userManager.isLoggedIn.value)

            vm.requestLogout()
            runCurrent()
            assertTrue(vm.uiState.value.showLogoutConfirm)

            vm.dismissLogout()
            runCurrent()
            assertFalse(vm.uiState.value.showLogoutConfirm)
            assertTrue(h.userManager.isLoggedIn.value)

            vm.requestLogout()
            vm.confirmLogout()
            runCurrent()

            assertFalse(vm.uiState.value.showLogoutConfirm)
            assertFalse(h.userManager.isLoggedIn.value)
        }
}
