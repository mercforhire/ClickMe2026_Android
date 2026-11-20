@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.PROFILE_HUB_ONLINE_DOT_TAG
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpertProfileHomeScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    @get:Rule(order = 2)
    val tmp = TemporaryFolder()

    private val calls = mutableMapOf<String, Int>()

    private fun count(name: String): () -> Unit = { calls[name] = (calls[name] ?: 0) + 1 }

    private val actions =
        ExpertProfileHubActions(
            onEditProfile = count("edit"),
            onSwitchToClient = count("switch"),
            onOpenExpertise = count("expertise"),
            onOpenAvailability = count("availability"),
            onOpenPayouts = count("payouts"),
            onOpenTaxProfile = count("tax"),
            onOpenSecurity = count("security"),
            onOpenNotifications = count("notifications"),
            onOpenHelp = count("help"),
            onOpenFeedback = count("feedback"),
            onLogOut = count("logout"),
        )

    private fun setContent() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileHomeContent(
                    card = ProfileHubCard("Ana Lee", "ana@example.com", null),
                    versionLabel = "Version 9.9.9 (42)",
                    actions = actions,
                    onViewPublicProfile = count("public"),
                )
            }
        }
    }

    private fun yOf(text: String): Float {
        val node = rule.onNodeWithText(text).fetchSemanticsNode()
        return node.positionInRoot.y
    }

    @Test
    fun expertHubShowsNoOnlineDot() {
        setContent()

        rule.onAllNodesWithTag(PROFILE_HUB_ONLINE_DOT_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun expertRowsInOrder() {
        setContent()

        val order =
            listOf(
                "Professional",
                "View My Public Profile",
                "Expertise & Topics",
                "Availability",
                "Payouts & Earnings",
                "Tax & Payout Info",
                "Account",
                "Security & Password",
                "Preferences",
                "Notifications",
                "Support",
                "FAQ & Help",
                "Send Feedback",
                "Privacy",
                "Log out",
            )
        val tops = order.map(::yOf)
        assertEquals(tops.sorted(), tops)
        assertEquals(order.size, tops.toSet().size)
        assertTrue(yOf("Switch to Client Mode") < yOf("Professional"))
        rule.onNodeWithText("Expert Settings").assertExists()
    }

    @Test
    fun expertRowsCallTheirActions() {
        setContent()

        rule.onNodeWithTag("profile_hub_user_card").performClick()
        rule.onNodeWithTag("profile_hub_mode_switch").performClick()
        listOf(
            "View My Public Profile",
            "Expertise & Topics",
            "Payouts & Earnings",
            "Tax & Payout Info",
            "Security & Password",
            "Notifications",
            "FAQ & Help",
            "Send Feedback",
        ).forEach { rule.onNodeWithText(it).performScrollTo().performClick() }
        rule.onNodeWithTag("profile_availability_row").performScrollTo().performClick()

        val expected =
            mapOf(
                "edit" to 1,
                "switch" to 1,
                "public" to 1,
                "expertise" to 1,
                "availability" to 1,
                "payouts" to 1,
                "tax" to 1,
                "security" to 1,
                "notifications" to 1,
                "help" to 1,
                "feedback" to 1,
            )
        assertEquals(expected, calls)
    }

    @Test
    fun expertHiddenRowsAbsent() {
        setContent()

        listOf(
            "Saved Experts",
            "Payment History",
            "Payment",
            "Discovery",
            "Pause Account",
            "coming soon",
        ).forEach { rule.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0) }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.signInAs(
        h: SignupTestHarness,
        id: String?,
    ) {
        if (id == null) return
        h.api.userProfileHandler = {
            UserProfileData(
                personalDetails =
                    UserProfileData.PersonalDetails(
                        firstName = "Ana",
                        lastName = "Lee",
                        email = "ana@example.com",
                        avatarUrl = "https://cdn.example/ana.jpg",
                    ),
                professionalDetails = UserProfileData.ProfessionalDetails(jobTitle = "Coach"),
                location = UserProfileData.Location(),
            )
        }
        h.api.loginHandler = {
            LoginData(
                token = "access-2",
                user = AuthUser(id = id, firstName = "Ana", lastName = "Lee", roles = listOf("expert")),
                refreshToken = "refresh-2",
                expiresAt = 1_900_000_100L,
                expiresIn = 3600L,
            )
        }
        h.userManager.login("ana@example.com", "abcdefg1")
        runCurrent()
    }

    @Test
    fun publicProfileKeyIsSelfView() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            signInAs(h, "u-1")
            val vm = ExpertProfileHomeViewModel(h.userManager)

            val key = vm.publicProfileKey()

            assertEquals(
                ExpertProfileKey(
                    expertId = "u-1",
                    name = "Ana Lee",
                    title = "Coach",
                    imageUrl = "https://cdn.example/ana.jpg",
                    isSelfView = true,
                ),
                key,
            )
        }

    @Test
    fun publicProfileKeyIsNullWithoutAUser() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val vm = ExpertProfileHomeViewModel(h.userManager)

            assertNull(vm.publicProfileKey())
        }

    @Test
    fun viewPublicProfileUsesTheKey() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            signInAs(h, "u-1")
            val vm = ExpertProfileHomeViewModel(h.userManager)
            val opened = mutableListOf<ExpertProfileKey>()

            rule.setContent {
                ClickMeTheme {
                    ExpertProfileHomeScreen(
                        actions = actions,
                        onOpenPublicProfile = { opened += it },
                        versionLabel = "Version 9.9.9 (42)",
                        viewModel = vm,
                    )
                }
            }
            rule.onNodeWithText("Ana Lee").assertExists()
            rule.onNodeWithText("View My Public Profile").performScrollTo().performClick()

            assertEquals(listOf(vm.publicProfileKey()), opened)
            assertTrue(opened.single().isSelfView)
            assertEquals("u-1", opened.single().expertId)
        }
}
