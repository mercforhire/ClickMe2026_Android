@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
    val rule = createAndroidComposeRule<ComponentActivity>()

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

    private fun systemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun farewellOutlivesForcedLogout() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            val state = AccountDeletionState()
            state.markDeleted()
            val vm = AccountDeletionFarewellViewModel(state, h.userManager)
            var route by mutableStateOf("Home")
            var routeTaps = 0
            // Mirrors ClickMeApp: a route-dependent child, then the Farewell host as the last child.
            rule.setContent {
                ClickMeTheme {
                    Box(Modifier.fillMaxSize()) {
                        Text(route, modifier = Modifier.clickable { routeTaps++ })
                        AccountDeletionFarewellHost(viewModel = vm)
                    }
                }
            }
            rule.onNodeWithText("We're sorry to see you go!").assertExists()

            // What a Rejected-driven logout does underneath the Farewell.
            h.userManager.logout()
            rule.runOnIdle { route = "Login" }
            rule.waitForIdle()

            rule.onNodeWithText("Login").assertExists()
            rule.onNodeWithText("We're sorry to see you go!").assertExists()
            // The Farewell is on top: a tap on the route child never reaches it.
            rule.onNodeWithText("Login").performClick()
            assertEquals(0, routeTaps)
        }

    @Test
    fun systemBackDoesNothing() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val state = AccountDeletionState()
            state.markDeleted()
            val vm = AccountDeletionFarewellViewModel(state, h.userManager)
            rule.setContent { ClickMeTheme { AccountDeletionFarewellHost(viewModel = vm) } }

            systemBack()

            rule.onNodeWithText("We're sorry to see you go!").assertExists()
            assertFalse(rule.activity.isFinishing)
            assertTrue(state.farewellVisible.value)
        }

    @Test
    fun backToLoginWhenAlreadyLoggedOut() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            h.userManager.logout()
            val clearsBefore = h.tokens.clearCount
            val state = AccountDeletionState()
            state.markDeleted()
            val vm = AccountDeletionFarewellViewModel(state, h.userManager)

            vm.backToLogin()
            advanceUntilIdle()

            assertEquals("no second logout", clearsBefore, h.tokens.clearCount)
            assertFalse(state.farewellVisible.value)
        }

    @Test
    fun farewellHiddenByDefault() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val vm = AccountDeletionFarewellViewModel(AccountDeletionState(), h.userManager)
            rule.setContent { ClickMeTheme { AccountDeletionFarewellHost(viewModel = vm) } }

            rule.onAllNodesWithText("We're sorry to see you go!").assertCountEquals(0)
            rule.onAllNodesWithText("Back to Login").assertCountEquals(0)
        }
}
