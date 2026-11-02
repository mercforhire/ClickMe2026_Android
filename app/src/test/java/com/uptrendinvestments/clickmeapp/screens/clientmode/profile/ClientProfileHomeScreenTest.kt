package com.uptrendinvestments.clickmeapp.screens.clientmode.profile

import android.app.Application
import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.config.AppLinks
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ClientProfileHomeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val calls = mutableMapOf<String, Int>()

    private fun count(name: String): () -> Unit = { calls[name] = (calls[name] ?: 0) + 1 }

    private val actions =
        ClientProfileHubActions(
            onEditProfile = count("edit"),
            onSwitchToExpert = count("switch"),
            onOpenSavedExperts = count("saved"),
            onOpenSecurity = count("security"),
            onOpenPayment = count("payment"),
            onOpenPaymentHistory = count("paymentHistory"),
            onOpenHelp = count("help"),
            onLogOut = count("logout"),
        )

    private val ana = ProfileHubCard("Ana Lee", "ana@example.com", null)

    private fun setContent(
        card: ProfileHubCard = ana,
        debug: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                ClientProfileHomeContent(
                    card = card,
                    versionLabel = "Version 9.9.9 (42)",
                    actions = actions,
                    debugContent = { if (debug) Text("debug-tools") },
                )
            }
        }
    }

    private fun yOf(text: String): Float {
        val node = rule.onNodeWithText(text).fetchSemanticsNode()
        return node.positionInRoot.y
    }

    @Test
    fun rowsInOrder() {
        setContent()

        val order =
            listOf(
                "Account",
                "Security & Password",
                "Payment",
                "Payment History",
                "Support",
                "FAQ & Help",
                "Privacy",
                "Log out",
            )
        val tops = order.map(::yOf)
        assertEquals(tops.sorted(), tops)
        assertEquals(order.size, tops.toSet().size)
        assertTrue(yOf("Switch to Expert Mode") < yOf("Account"))
        rule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun hiddenRowsAbsent() {
        setContent()

        listOf(
            "Notifications",
            "Send Feedback",
            "Pause Account",
            "coming soon",
        ).forEach { rule.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0) }
        rule.onAllNodesWithText("Professional").assertCountEquals(0)
    }

    @Test
    fun discoverySectionSitsBetweenModeSwitchAndAccount() {
        setContent()

        rule.onNodeWithText("Discovery").assertIsDisplayed()
        rule.onNodeWithText("Saved Experts").assertIsDisplayed()
        val order = listOf("Switch to Expert Mode", "Discovery", "Saved Experts", "Account")
        val tops = order.map(::yOf)
        assertEquals(tops.sorted(), tops)
        assertEquals(order.size, tops.toSet().size)

        rule.onNodeWithText("Saved Experts").performClick()

        assertEquals(mapOf("saved" to 1), calls)
    }

    @Test
    fun rowsCallTheirActions() {
        setContent()

        rule.onNodeWithTag("profile_hub_user_card").performClick()
        rule.onNodeWithTag("profile_hub_mode_switch").performClick()
        listOf("Security & Password", "Payment", "Payment History", "FAQ & Help").forEach {
            rule.onNodeWithText(it).performScrollTo().performClick()
        }

        assertEquals(
            mapOf("edit" to 1, "switch" to 1, "security" to 1, "payment" to 1, "paymentHistory" to 1, "help" to 1),
            calls,
        )
    }

    @Test
    fun userCardFallbacks() {
        setContent(card = ProfileHubCard(null, null, null))

        rule.onNodeWithText("Your Profile").assertExists()
        rule.onAllNodesWithText("@", substring = true).assertCountEquals(0)
        rule
            .onNodeWithTag("profile_hub_user_card")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription("Your Profile, Edit profile").assertExists()
    }

    @Test
    fun userCardWithEmailSpeaksNameEmailAndAction() {
        setContent()

        rule.onNodeWithText("Ana Lee").assertExists()
        rule.onNodeWithText("ana@example.com").assertExists()
        rule.onNodeWithContentDescription("Ana Lee, ana@example.com, Edit profile").assertExists()
    }

    @Test
    fun logOutAsksFirst() {
        setContent()

        rule.onNodeWithTag("profile_hub_logout").performScrollTo().performClick()
        rule.onNodeWithText("Log out of ClickMe?").assertExists()
        assertEquals(null, calls["logout"])

        rule.onNodeWithText("Stay logged in").performClick()
        rule.onAllNodesWithText("Log out of ClickMe?").assertCountEquals(0)
        assertEquals(null, calls["logout"])

        rule.onNodeWithTag("profile_hub_logout").performScrollTo().performClick()
        rule.onNodeWithText("Log Out").performClick()
        rule.onAllNodesWithText("Log out of ClickMe?").assertCountEquals(0)
        assertEquals(1, calls["logout"])
    }

    @Test
    fun logOutNeverAutoConfirmsAfterRestore() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                ClientProfileHomeContent(card = ana, versionLabel = "Version 9.9.9 (42)", actions = actions)
            }
        }
        rule.onNodeWithTag("profile_hub_logout").performScrollTo().performClick()
        rule.onNodeWithText("Log out of ClickMe?").assertExists()

        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // The confirm dialog may come back, but only a tap on "Log Out" runs the callback.
        assertEquals(null, calls["logout"])
        rule.onNodeWithText("Stay logged in").performClick()
        assertEquals(null, calls["logout"])
        rule.onAllNodesWithText("Log out of ClickMe?").assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun hubCapped() {
        setContent()

        val card = rule.onNodeWithTag("profile_hub_user_card").getUnclippedBoundsInRoot()
        assertTrue("card width ${card.right - card.left}", card.right - card.left <= 600.dp)
        assertTrue("card left ${card.left}", card.left >= 150.dp)
    }

    @Test
    fun privacyOpensCustomTab() {
        setContent()
        val application = ApplicationProvider.getApplicationContext<Application>()

        rule.onNodeWithText("Privacy").performScrollTo().performClick()

        val started: Intent? = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(AppLinks.PRIVACY_POLICY, started?.data?.toString())
    }

    @Test
    fun versionFooterAndDebugSlot() {
        setContent(debug = true)

        rule.onNodeWithText("Version 9.9.9 (42)").assertExists()
        rule.onNodeWithText("© 2026 ClickMe Innovations").assertExists()
        rule.onNodeWithText("debug-tools").assertExists()
        assertTrue(yOf("debug-tools") > yOf("© 2026 ClickMe Innovations"))
    }
}
