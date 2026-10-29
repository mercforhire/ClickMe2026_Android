@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDeletionFarewellTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val tmp = TemporaryFolder()

    @get:Rule(order = 2)
    val rule = createComposeRule()

    @Test
    fun backToLoginLogsOutThenHides() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            val state = AccountDeletionState()
            state.markDeleted()
            val vm = AccountDeletionFarewellViewModel(state, h.userManager)
            val seen = mutableListOf<Pair<Boolean, Boolean>>()
            val job =
                backgroundScope.launch(main.testDispatcher) {
                    combine(h.userManager.isLoggedIn, state.farewellVisible) { loggedIn, visible ->
                        loggedIn to visible
                    }.collect { seen += it }
                }

            vm.backToLogin()
            advanceUntilIdle()
            job.cancel()

            assertEquals(false to false, seen.last())
            assertTrue("logged out while the farewell was still up", (false to true) in seen)
            assertFalse("the farewell hid before the logout finished", (true to false) in seen)
        }

    @Test
    fun farewellRendersTheIosCopy() {
        var taps = 0
        rule.setContent {
            ClickMeTheme { DeleteAccountFarewell(onBackToLogin = { taps++ }) }
        }

        rule.onNodeWithText("We're sorry to see you go!").assertExists()
        rule
            .onNodeWithText("Your account has been successfully deleted. We hope to see you again soon.")
            .assertExists()
        rule.onNodeWithText("Back to Login").assertExists()
        rule.onNodeWithTag("delete_back_to_login").performClick()

        assertEquals(1, taps)
    }
}
