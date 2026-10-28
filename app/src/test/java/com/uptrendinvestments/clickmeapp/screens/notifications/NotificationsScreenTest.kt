package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NOTIFICATION_ROW_TAG
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
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

    private fun show(state: NotificationsUiState) {
        rule.setContent {
            ClickMeTheme {
                NotificationsContent(
                    state = state,
                    onBack = { backCount++ },
                    onRowTap = { tapped = it },
                )
            }
        }
    }

    private val threeRows =
        loaded(
            NotificationSection(NotificationDay.Today, listOf(row("1", "First"), row("2", "Second"))),
            NotificationSection(NotificationDay.Earlier, listOf(row("3", "Third"))),
        )

    @Test
    fun loadedRowsUnderHeaders() {
        show(threeRows)
        rule.onNodeWithText("Today").assertExists()
        rule.onNodeWithText("Earlier").assertExists()
        assertTrue(rule.onAllNodesWithText("Yesterday").fetchSemanticsNodes().isEmpty())
        val rows = rule.onAllNodesWithTag(NOTIFICATION_ROW_TAG)
        assertEquals(3, rows.fetchSemanticsNodes().size)
        listOf("First", "Second", "Third").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun headersHaveHeadingSemantics() {
        show(threeRows)
        val node = rule.onNodeWithText("Today").fetchSemanticsNode()
        assertTrue(node.config.getOrNull(SemanticsProperties.Heading) != null)
    }

    @Test
    fun appBarTitleAndBack() {
        show(threeRows)
        rule.onNodeWithText("Notifications").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }
}
