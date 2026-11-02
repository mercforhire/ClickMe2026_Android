@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.AnnotatedString
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferencesData
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.allNotificationRows
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationSettingsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val toggles = mutableListOf<Pair<String, Boolean>>()
    private val loadRetries = mutableListOf<Unit>()
    private val saveRetries = mutableListOf<Unit>()
    private val backs = mutableListOf<Unit>()

    private val allRowTitles =
        listOf(
            "Booking Requests",
            "Booking Confirmations",
            "Booking Declines",
            "Booking Cancellations",
            "Booking Reschedules",
            "Booking Expirations",
            "Refunds",
            "Session Reminders",
            "Partner No-Show",
            "Session Ended",
            "New Messages",
            "New Reviews",
            "Review Reminders",
        )

    private fun loaded(values: Map<String, Boolean> = defaultNotificationValues()) =
        NotificationSettingsUiState(load = NotificationLoad.Loaded, values = values)

    private fun setContent(state: NotificationSettingsUiState) {
        rule.setContent {
            ClickMeTheme {
                NotificationSettingsContent(
                    state = state,
                    onToggle = { key, on -> toggles += key to on },
                    onRetry = { loadRetries += Unit },
                    onRetrySave = { saveRetries += Unit },
                    onBack = { backs += Unit },
                )
            }
        }
    }

    @Test
    fun titleSectionsAndRowsShow() {
        setContent(loaded())

        rule.onNodeWithText("Notifications").assertExists()
        val headings = listOf("Bookings", "Sessions", "Messages", "Reviews")
        val tops =
            headings.map { rule.onNodeWithText(it).getUnclippedBoundsInRoot().top }
        assertEquals(tops.sorted(), tops)
        rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).assertCountEquals(13)
        for (title in allRowTitles) rule.onNodeWithText(title).assertExists()
        rule.onAllNodesWithText("In-app, Email, Push").assertCountEquals(13)
    }

    @Test
    fun noQuietHoursRow() {
        setContent(loaded())

        rule.onAllNodesWithText("Quiet", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun oneRowSectionHasNoDivider() {
        setContent(loaded())

        val cards = rule.onAllNodesWithTag(NOTIFICATION_SECTION_CARD_TAG)
        cards.assertCountEquals(4)
        val messages = hasTestTag(NOTIFICATION_SECTION_CARD_TAG) and hasAnyDescendantText("New Messages")
        rule.onNode(hasTestTag(NOTIFICATION_ROW_TAG) and hasAnyAncestor(messages)).assertExists()
        rule.onAllNodes(hasTestTag(NOTIFICATION_DIVIDER_TAG) and hasAnyAncestor(messages)).assertCountEquals(0)
        // 6 + 2 + 0 + 1 dividers across the four cards.
        rule.onAllNodesWithTag(NOTIFICATION_DIVIDER_TAG).assertCountEquals(9)
    }

    private fun hasAnyDescendantText(text: String): SemanticsMatcher =
        SemanticsMatcher("has descendant text $text") { node ->
            fun walk(n: SemanticsNode): Boolean =
                n.children.any { child ->
                    val texts: List<AnnotatedString> = child.config.getOrNull(SemanticsProperties.Text).orEmpty()
                    texts.any { it.text == text } || walk(child)
                }
            walk(node)
        }

    @Test
    fun tappingARowTogglesThatKey() {
        setContent(loaded())

        rule.onNodeWithText("Partner No-Show").performScrollTo().performClick()

        assertEquals(listOf("session|no_show" to false), toggles)
    }

    @Test
    fun rowIsOneSwitchNode() {
        setContent(loaded())

        val row = rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).onFirst()
        val config = row.fetchSemanticsNode().config
        assertEquals(Role.Switch, config[SemanticsProperties.Role])
        assertTrue(config.contains(SemanticsProperties.ToggleableState))
        val texts = config[SemanticsProperties.Text].map { it.text }
        assertTrue(texts.contains("Booking Requests"))
        assertTrue(texts.contains("In-app, Email, Push"))
        // The Switch itself carries no toggle semantics, so no second focusable switch exists.
        rule
            .onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag(NOTIFICATION_ROW_TAG)), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun aRowTheServerOmittedRendersOn() {
        // `values` is built from the grid defaults, so a row the server omitted is on.
        setContent(loaded())

        rule.onNodeWithText("Refunds").assertIsOn()
    }

    @Test
    fun anOffRowRendersOff() {
        setContent(loaded(defaultNotificationValues() + ("booking|refunds" to false)))

        rule.onNodeWithText("Refunds").assertIsOff()
        rule.onNodeWithText("Session Ended").assertIsOn()
    }

    @Test
    fun leavingFlushesAPendingEdit() =
        runTest(main.testDispatcher) {
            val api = FakeClickMeApi()
            api.notificationPreferencesHandler = { NotificationPreferencesData(allNotificationRows()) }
            val vm = NotificationSettingsViewModel(NotificationSettingsRepository(api), backgroundScope)
            runCurrent()
            var shown by mutableStateOf(true)
            rule.setContent {
                ClickMeTheme {
                    if (shown) NotificationSettingsScreen(viewModel = vm, onBack = { backs += Unit })
                }
            }

            rule.onNodeWithText("Partner No-Show").performScrollTo().performClick()
            assertEquals(0, api.updateNotificationPreferencesCalls)
            rule.runOnIdle { shown = false }
            rule.waitForIdle()
            runCurrent()

            assertEquals(1, api.updateNotificationPreferencesCalls)
            val sent = api.updateNotificationPreferencesRequests.single().preferences
            assertEquals(false, sent.single { it.subCategory == "no_show" }.pushEnabled)
        }

    @Test
    fun backArrowFlushesThenLeaves() =
        runTest(main.testDispatcher) {
            val api = FakeClickMeApi()
            api.notificationPreferencesHandler = { NotificationPreferencesData(allNotificationRows()) }
            val vm = NotificationSettingsViewModel(NotificationSettingsRepository(api), backgroundScope)
            runCurrent()
            rule.setContent {
                ClickMeTheme {
                    NotificationSettingsScreen(viewModel = vm, onBack = { backs += Unit })
                }
            }

            rule.onNodeWithText("Session Ended").performScrollTo().performClick()
            rule.onNodeWithContentDescription("Back").performClick()
            runCurrent()

            assertEquals(1, api.updateNotificationPreferencesCalls)
            assertEquals(1, backs.size)
        }
}
