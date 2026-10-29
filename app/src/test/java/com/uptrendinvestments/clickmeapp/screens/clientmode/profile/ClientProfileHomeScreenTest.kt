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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
            onOpenSecurity = count("security"),
            onOpenPayment = count("payment"),
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

    private fun yOf(text: String): Float = rule.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

    @Test
    fun rowsInOrder() {
        setContent()

        val order =
            listOf(
                "Account",
                "Security & Password",
                "Payment",
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
            "Saved Experts",
            "Payment History",
            "Notifications",
            "Send Feedback",
            "Discovery",
            "Pause Account",
            "coming soon",
        ).forEach { rule.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0) }
        rule.onAllNodesWithText("Professional").assertCountEquals(0)
    }

    @Test
    fun rowsCallTheirActions() {
        setContent()

        rule.onNodeWithTag("profile_hub_user_card").performClick()
        rule.onNodeWithTag("profile_hub_mode_switch").performClick()
        listOf("Security & Password", "Payment", "FAQ & Help").forEach {
            rule.onNodeWithText(it).performScrollTo().performClick()
        }

        assertEquals(mapOf("edit" to 1, "switch" to 1, "security" to 1, "payment" to 1, "help" to 1), calls)
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
