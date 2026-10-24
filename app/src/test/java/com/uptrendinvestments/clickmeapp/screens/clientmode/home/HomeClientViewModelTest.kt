@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeClientViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val guard = FakeCallGuard()

    @Test
    fun hasSeenOnboardingStartsFalseAndMirrorsUserManager() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val vm = HomeClientViewModel(h.userManager, guard)

            assertFalse(vm.hasSeenOnboarding.value)
            h.userManager.markOnboardingShown()
            assertTrue(vm.hasSeenOnboarding.value)
        }

    @Test
    fun markOnboardingShownFlipsFlagAndPersistsIt() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val vm = HomeClientViewModel(h.userManager, guard)

            vm.markOnboardingShown()
            runCurrent()

            assertTrue(vm.hasSeenOnboarding.value)
            assertTrue(h.sessionStore.hasSeenOnboarding())
        }

    @Test
    fun logoutDuringACallIsBlockedAndKeepsTheSession() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            val vm = HomeClientViewModel(h.userManager, guard)
            guard.blocked = true

            vm.logout()
            runCurrent()

            assertTrue(h.userManager.isLoggedIn.value)
            assertEquals(listOf(RestrictedAction.LogOut), guard.attempts)
        }

    @Test
    fun logoutWithoutACallEndsTheSession() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            val vm = HomeClientViewModel(h.userManager, guard)

            vm.logout()
            runCurrent()

            assertFalse(h.userManager.isLoggedIn.value)
            assertEquals(listOf(RestrictedAction.LogOut), guard.attempts)
        }
}
