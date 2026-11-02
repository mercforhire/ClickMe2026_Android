@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferencesData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
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
import org.robolectric.annotation.Config

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

    private fun withStatus(status: AutoSaveStatus) = loaded().copy(status = status)

    @Test
    fun loadingShowsTheSkeletonAndNoStatus() {
        setContent(NotificationSettingsUiState(load = NotificationLoad.Loading, status = AutoSaveStatus.Saving))

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).assertCountEquals(0)
        rule.onNodeWithTag(NOTIFICATION_SAVE_STATUS_TAG).assertExists()
        rule.onNodeWithContentDescription("Saving").assertDoesNotExist()
    }

    @Test
    fun loadFailureShowsRetryAndNoStatus() {
        setContent(
            NotificationSettingsUiState(
                load = NotificationLoad.Failed(FieldError.Text("Server unreachable")),
                status = AutoSaveStatus.Error,
            ),
        )

        rule.onNodeWithText("Couldn't load preferences").assertExists()
        rule.onNodeWithText("Server unreachable").assertExists()
        rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).assertCountEquals(0)
        rule.onNodeWithContentDescription("Couldn't save preferences. Retry").assertDoesNotExist()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, loadRetries.size)
        assertEquals(0, saveRetries.size)
    }

    @Test
    fun idleStatusIsAnEmptySlot() {
        setContent(withStatus(AutoSaveStatus.Idle))

        val slot = rule.onNodeWithTag(NOTIFICATION_SAVE_STATUS_TAG)
        slot.assertWidthIsEqualTo(48.dp)
        slot.assertHeightIsEqualTo(48.dp)
        rule.onNodeWithContentDescription("Saving").assertDoesNotExist()
        rule.onNodeWithContentDescription("Saved").assertDoesNotExist()
    }

    @Test
    fun savingShowsASpinnerDescribedSaving() {
        setContent(withStatus(AutoSaveStatus.Saving))

        rule.onNodeWithContentDescription("Saving").assertExists()
    }

    @Test
    fun savedShowsACheckDescribedSaved() {
        setContent(withStatus(AutoSaveStatus.Saved))

        val node = rule.onNodeWithContentDescription("Saved")
        node.assertExists()
        assertEquals(LiveRegionMode.Polite, node.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
    }

    @Test
    fun errorShowsRetryAndCallsIt() {
        setContent(withStatus(AutoSaveStatus.Error))

        val group = rule.onNodeWithContentDescription("Couldn't save preferences. Retry")
        group.assertExists()
        assertEquals(LiveRegionMode.Polite, group.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, saveRetries.size)
        assertEquals(0, loadRetries.size)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun togglesStayEnabledWhileSaving() {
        setContent(withStatus(AutoSaveStatus.Saving))

        rule
            .onNodeWithText("Booking Requests")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf("booking|requests" to false), toggles)
    }

    @Test
    fun errorKeepsTheToggleAsSet() {
        setContent(
            loaded(defaultNotificationValues() + ("booking|declines" to false)).copy(status = AutoSaveStatus.Error),
        )

        rule.onNodeWithText("Booking Declines").assertIsOff()
    }

    @Test
    fun theBarDoesNotShiftBetweenStates() {
        var state by mutableStateOf(withStatus(AutoSaveStatus.Idle))
        rule.setContent {
            ClickMeTheme {
                NotificationSettingsContent(
                    state = state,
                    onToggle = { _, _ -> },
                    onRetry = {},
                    onRetrySave = {},
                    onBack = {},
                )
            }
        }

        fun positions() =
            rule.onNodeWithText("Notifications").getUnclippedBoundsInRoot().left to
                rule.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot().left

        val idle = positions()
        for (status in listOf(AutoSaveStatus.Saving, AutoSaveStatus.Saved, AutoSaveStatus.Error, AutoSaveStatus.Idle)) {
            rule.runOnIdle { state = withStatus(status) }
            rule.waitForIdle()
            assertEquals("status $status", idle, positions())
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun largeFontRowsWrapAndTheSwitchStays() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ClickMeTheme {
                    NotificationSettingsContent(
                        state = loaded(),
                        onToggle = { _, _ -> },
                        onRetry = {},
                        onRetrySave = {},
                        onBack = {},
                    )
                }
            }
        }

        val row = rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).onFirst().getUnclippedBoundsInRoot()
        val switch =
            rule
                .onAllNodesWithTag(NOTIFICATION_SWITCH_TAG, useUnmergedTree = true)
                .onFirst()
                .getUnclippedBoundsInRoot()
        assertTrue("switch left ${switch.left} inside row ${row.left}", switch.left >= row.left)
        assertTrue("switch right ${switch.right} inside row ${row.right}", switch.right <= row.right)
        assertTrue("row at least 56dp tall", row.bottom - row.top >= 56.dp)
    }
}
