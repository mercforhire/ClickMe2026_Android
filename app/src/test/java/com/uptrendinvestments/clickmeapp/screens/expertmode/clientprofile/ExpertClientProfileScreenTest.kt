@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.expertClientProfile
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A tall window composes every lazy item at once.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ExpertClientProfileScreenTest {
    private val main = MainDispatcherRule(UnconfinedTestDispatcher())
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(main).around(composeRule)

    private val rule get() = composeRule

    private val api = FakeClickMeApi()
    private var backCount = 0

    private fun stub(profile: ExpertClientProfileData = expertClientProfile()) {
        api.getExpertClientHandler = { profile }
        api.expertBookingsHandler = { ExpertBookingsPayload(bookings = emptyList()) }
    }

    private fun vm() =
        ExpertClientProfileViewModel(
            ExpertClientProfileKey("c1", ExpertBookingOrigin.Bookings),
            ExpertClientProfileRepository(api),
            ExpertBookingsRepository(api),
        )

    private fun show(viewModel: ExpertClientProfileViewModel = vm()) {
        rule.setContent {
            ClickMeTheme { ExpertClientProfileScreen(viewModel = viewModel, onBack = { backCount++ }) }
        }
    }

    private fun shownCount(text: String) =
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aLoadedProfileShowsTheBarNameAndBadge() {
        stub(expertClientProfile(fullName = "Sophia Carter"))
        show()
        rule.onNodeWithText("Client Profile").assertExists()
        assertEquals(1, shownCount("Sophia Carter"))
        assertEquals(1, shownCount("Client"))
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun aNullNameFallsBackToClient() {
        stub(expertClientProfile(fullName = null))
        show()
        // The fallback name and the badge both read "Client".
        assertEquals(2, shownCount("Client"))
    }

    @Test
    fun anAvatarUrlDoesNotBlockTheScreen() {
        stub(expertClientProfile(fullName = "Sophia Carter"))
        show()
        assertTrue(shownCount("Sophia Carter") == 1)
        assertTrue(shownCount("Client") == 1)
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}
