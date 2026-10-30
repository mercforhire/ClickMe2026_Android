package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NOTIFICATIONS_END_NOTE_TAG
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NOTIFICATIONS_OFF_ROW_TAG
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NOTIFICATION_BODY_TAG
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NOTIFICATION_ROW_TAG
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.categoryIcon
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.isInteractiveCategory
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

// A tall window composes every lazy item at once.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class NotificationsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var backCount = 0
    private var settingsCount = 0
    private var retryCount = 0
    private var refreshCount = 0
    private var refreshFailureShownCount = 0
    private var tapped: NotificationRowModel? = null

    private fun row(
        id: String,
        title: String,
        body: String = "Body of $title.",
        category: String = "booking",
        time: NotificationTime = NotificationTime.MinutesAgo(2),
        destination: PushDestination? = PushDestination.BookingDetail("b-$id", PushAudience.Client),
    ) = NotificationRowModel(id, category, title, body, Instant.EPOCH, time, destination)

    private fun loaded(vararg sections: NotificationSection) =
        NotificationsUiState(load = NotificationsLoad.Loaded(sections.toList()))

    private fun today(vararg rows: NotificationRowModel) = NotificationSection(NotificationDay.Today, rows.toList())

    private fun show(
        state: NotificationsUiState,
        notificationsEnabled: Boolean = true,
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ClickMeTheme {
                    NotificationsContent(
                        state = state,
                        notificationsEnabled = notificationsEnabled,
                        onBack = { backCount++ },
                        onRetry = { retryCount++ },
                        onRefresh = { refreshCount++ },
                        onRowTap = { tapped = it },
                        onOpenSettings = { settingsCount++ },
                        onRefreshFailureShown = { refreshFailureShownCount++ },
                    )
                }
            }
        }
    }

    private val threeRows =
        loaded(
            today(row("1", "First"), row("2", "Second")),
            NotificationSection(NotificationDay.Earlier, listOf(row("3", "Third"))),
        )

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun loadedRowsUnderHeaders() {
        show(threeRows)
        rule.onNodeWithText("Today").assertExists()
        rule.onNodeWithText("Earlier").assertExists()
        assertFalse(exists("Yesterday"))
        assertEquals(3, rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).fetchSemanticsNodes().size)
        listOf("First", "Second", "Third").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun headersHaveHeadingSemantics() {
        show(threeRows)
        val node = rule.onNodeWithText("Today").fetchSemanticsNode()
        assertNotNull(node.config.getOrNull(SemanticsProperties.Heading))
    }

    @Test
    fun appBarTitleAndBack() {
        show(threeRows)
        rule.onNodeWithText("Notifications").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun tappableRowIsOneButton() {
        val booking = row("1", "Booking confirmed", body = "Your session with **Ada** is confirmed.")
        show(loaded(today(booking)))
        val node =
            rule.onNode(hasContentDescription("Booking confirmed, Your session with Ada is confirmed., 2 min ago"))
        node.assertHasClickAction()
        val config = node.fetchSemanticsNode().config
        assertEquals(Role.Button, config[SemanticsProperties.Role])
        assertEquals("Open notification", config[SemanticsActions.OnClick].label)
        node.performClick()
        assertEquals(booking, tapped)
    }

    @Test
    fun inertRowHasNoClickActionAndNoRole() {
        show(loaded(today(row("1", "Payout sent", category = "payout", destination = null))))
        val node = rule.onNodeWithTag(NOTIFICATION_ROW_TAG)
        val config = node.fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsActions.OnClick))
        assertNull(config.getOrNull(SemanticsProperties.Role))
        rule.onNode(hasTestTag(NOTIFICATION_ROW_TAG) and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun blankBodyShowsTitleOnly() {
        show(loaded(today(row("1", "Just a title", body = ""))))
        rule.onNodeWithText("Just a title").assertExists()
        assertTrue(
            rule.onAllNodesWithTag(NOTIFICATION_BODY_TAG, useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun boldSpansRender() {
        show(loaded(today(row("1", "T", body = "Your session with **Ada** is confirmed."))))
        val text =
            rule
                .onNodeWithTag(NOTIFICATION_BODY_TAG, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .first()
        assertEquals("Your session with Ada is confirmed.", text.text)
        assertFalse(text.text.contains("**"))
        val bold = text.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals("Ada", text.text.substring(bold.start, bold.end))
    }

    @Test
    fun categoryTilesUseTheSpecGlyphsAndTints() {
        assertSame(Icons.Outlined.CalendarMonth, categoryIcon("booking"))
        assertSame(Icons.Outlined.Timer, categoryIcon("session"))
        assertSame(Icons.Outlined.ChatBubbleOutline, categoryIcon("message"))
        assertSame(Icons.Outlined.StarBorder, categoryIcon("review"))
        assertSame(Icons.Outlined.Payments, categoryIcon("payout"))
        assertSame(Icons.Outlined.Shield, categoryIcon("account"))
        assertSame(Icons.Outlined.NotificationsNone, categoryIcon("promo"))
        listOf("booking", "session", "message", "review").forEach { assertTrue(isInteractiveCategory(it)) }
        listOf("payout", "account", "promo").forEach { assertFalse(isInteractiveCategory(it)) }
    }

    @Test
    fun endNoteAfterLastSection() {
        show(threeRows)
        val note = rule.onNodeWithTag(NOTIFICATIONS_END_NOTE_TAG)
        note.assertExists()
        rule.onNodeWithText("End of recent updates").assertExists()
        val lastRowTop = rule.onNodeWithText("Third").getUnclippedBoundsInRoot().top
        assertTrue(note.getUnclippedBoundsInRoot().top > lastRowTop)
    }

    @Test
    fun offRowShowsWhenNotificationsAreDisabled() {
        show(threeRows, notificationsEnabled = false)
        val node = rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG)
        node.assertHasClickAction()
        val config = node.fetchSemanticsNode().config
        assertEquals(Role.Button, config[SemanticsProperties.Role])
        assertEquals("Open notification settings", config[SemanticsActions.OnClick].label)
        rule.onNode(hasContentDescription("Notifications are off — Turn on")).assertExists()
        rule.onNodeWithText("Turn on").assertExists()
        // The first item: above the first section header.
        assertTrue(node.getUnclippedBoundsInRoot().top < rule.onNodeWithText("Today").getUnclippedBoundsInRoot().top)
        node.performClick()
        assertEquals(1, settingsCount)
    }

    @Test
    fun offRowIsAbsentWhenNotificationsAreEnabled() {
        show(threeRows, notificationsEnabled = true)
        rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun offRowIsAlsoShownForAnEmptyLoadedList() {
        show(loaded(), notificationsEnabled = false)
        rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG).assertExists()
    }

    @Test
    fun loadingShowsSkeletonWithoutOffRow() {
        show(NotificationsUiState(load = NotificationsLoad.Loading), notificationsEnabled = false)
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG).assertDoesNotExist()
        rule.onNodeWithTag(NOTIFICATIONS_LIST_TAG).assertDoesNotExist()
    }

    @Test
    fun failedShowsErrorWithRetry() {
        show(
            NotificationsUiState(load = NotificationsLoad.Failed(FieldError.Text("Boom"))),
            notificationsEnabled = false,
        )
        rule.onNodeWithText("Couldn't load notifications").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithTag(NOTIFICATIONS_RETRY_TAG).assertExists()
        rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG).assertDoesNotExist()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun failedWithNetworkLine() {
        show(NotificationsUiState(load = NotificationsLoad.Failed(FieldError.Res(R.string.login_error_network))))
        rule.onNodeWithText("Check your connection and try again.").assertExists()
    }

    @Test
    fun emptyStateWithOffRow() {
        show(loaded(), notificationsEnabled = false)
        rule.onNodeWithText("You're all caught up").assertExists()
        rule.onNodeWithText("No new notifications right now.").assertExists()
        val off = rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG)
        assertTrue(
            off.getUnclippedBoundsInRoot().top <
                rule.onNodeWithText("You're all caught up").getUnclippedBoundsInRoot().top,
        )
    }

    @Test
    fun emptyStateWithoutOffRowWhenEnabled() {
        show(loaded(), notificationsEnabled = true)
        rule.onNodeWithText("You're all caught up").assertExists()
        rule.onNodeWithTag(NOTIFICATIONS_OFF_ROW_TAG).assertDoesNotExist()
        assertFalse(exists("End of recent updates"))
    }

    @Test
    fun oneRowOneSection() {
        show(loaded(today(row("1", "Only one"))))
        assertEquals(1, rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).fetchSemanticsNodes().size)
        rule.onNodeWithText("Today").assertExists()
        assertFalse(exists("Earlier"))
    }

    @Test
    fun refreshFailureShowsSnackbarAndIsConsumedOnce() {
        show(loaded(today(row("1", "A"))).copy(refreshFailed = true))
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, refreshFailureShownCount)
    }

    @Test
    fun refreshFailedShowsRetry() {
        show(loaded(today(row("1", "A"), row("2", "B"))).copy(refreshFailed = true))
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, refreshCount)
        assertEquals(0, retryCount)
        assertEquals(2, rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun listCells() {
        show(NotificationsUiState(load = NotificationsLoad.Loading))
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun listCellsEmptyAndNamedError() {
        show(loaded())
        rule.onNodeWithText("You're all caught up").assertExists()
    }

    @Test
    fun listCellsNamedErrorHasRetry() {
        show(NotificationsUiState(load = NotificationsLoad.Failed(FieldError.Text("Boom"))))
        rule.onNodeWithText("Couldn't load notifications").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun listCellsOneRowHasNoEmptyState() {
        show(loaded(today(row("1", "Only one"))))
        rule.onNodeWithText("Only one").assertExists()
        assertFalse(exists("You're all caught up"))
    }

    @Test
    @Config(qualifiers = "w900dp-h3000dp")
    fun listCapped() {
        show(threeRows)
        val bounds = rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG)[0].getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        assertTrue("row width $width", width <= 600.dp)
        // The list scroll area itself stays full width.
        val list = rule.onNodeWithTag(NOTIFICATIONS_LIST_TAG).getUnclippedBoundsInRoot()
        assertTrue(list.right - list.left > 600.dp)
    }

    @Test
    fun largeFontWraps() {
        val long = "A very long notification title that needs several lines at the largest font size"
        val longBody = "A body that is also long enough to wrap across many lines when the font is doubled in size."
        show(loaded(today(row("1", long, body = longBody))), notificationsEnabled = false, fontScale = 2f)
        rule.onNodeWithText(long).assertExists()
        rule.onNodeWithText(longBody).assertExists()
        rule.onNodeWithText("Turn on").assertExists()
        listOf(long, longBody).forEach { text ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            rule
                .onNodeWithText(text, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult]
                .action
                ?.invoke(layouts)
            assertEquals(1, layouts.size)
            // Robolectric's text metrics are not real, so check the configuration: no line limit, no ellipsis.
            val input = layouts.single().layoutInput
            assertEquals(Int.MAX_VALUE, input.maxLines)
            assertEquals(TextOverflow.Clip, input.overflow)
        }
    }
}
