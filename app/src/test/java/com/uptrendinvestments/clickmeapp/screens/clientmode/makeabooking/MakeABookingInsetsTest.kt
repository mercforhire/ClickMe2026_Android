package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.NOTES_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.TOPIC_PICKER_TAG
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * POL-03: with the keyboard open on the Notes field the Book Now button is still reachable above it, and the form
 * column is capped on a large window while the scroll area stays full width (D-02).
 */
@RunWith(RobolectricTestRunner::class)
class MakeABookingInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private val slot = Instant.parse("2026-10-22T20:00:00Z")

    private fun bookingState(): MakeABookingUiState {
        val date = LocalDate.of(2026, 10, 22)
        return MakeABookingUiState(
            expertId = "e1",
            expertName = "Ada Lovelace",
            expertTitle = "Engineer",
            expertImageUrl = null,
            zone = ZoneId.of("America/Toronto"),
            today = date,
            now = Instant.parse(NOW),
            selectedDate = date,
            displayedMonth = YearMonth.of(2026, 10),
            topics =
                TopicsState.Loaded(listOf(topicDto("t1", isFree = true, durationMins = 60).toBookingTopicUi())),
            selectedTopicId = "t1",
            availability =
                AvailabilityState.Loaded(
                    mapOf(date to listOf(BookingTimeSlot(slot, slot.plusSeconds(1800), isAvailable = true))),
                ),
            expertTimezone = "America/Vancouver",
            selectedSlotStart = slot,
            meetingType = MeetingType.InAppVoice,
        )
    }

    private fun show() {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                MakeABookingContent(
                    state = bookingState(),
                    onBack = {},
                    onRetryTopics = {},
                    onSelectTopic = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectDate = {},
                    onSelectSlot = {},
                    onRetryAvailability = {},
                    onSelectMeetingType = {},
                    onNotesChange = {},
                    onBook = {},
                    onOpenPaymentMethods = {},
                    onDismissError = {},
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun bookNowAboveImeWhileTypingNotes() {
        show()
        rule.dispatchInsets(view, imeInsets(bottomPx = 300))
        rule.onNodeWithTag(NOTES_FIELD_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        // The form scrolls inside the IME-shrunk viewport; bring Book Now into it, then measure against the IME top.
        val bookNow = rule.onNodeWithTag(BOOK_NOW_TAG)
        bookNow.performScrollTo()
        bookNow.assertIsDisplayed()
        val bottom = bookNow.getUnclippedBoundsInRoot().bottom.value
        assertTrue("Book Now bottom $bottom dp must be at or above the IME top (500 dp)", bottom <= 500f)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun formCappedOnLargeWindow() {
        show()

        val topic = rule.onNodeWithTag(TOPIC_PICKER_TAG).getUnclippedBoundsInRoot()
        val width = topic.right.value - topic.left.value
        val center = (topic.left.value + topic.right.value) / 2f
        assertTrue("Topic row is $width dp wide, expected <= 600", width <= 600f)
        assertEquals("Topic row must stay centered on the 900 dp window", 450f, center, 1f)

        // The scroll area itself (and so its drag and tap handling) still spans the whole window.
        val scroll = rule.onNodeWithTag(MAKE_A_BOOKING_SCROLL_TAG).getUnclippedBoundsInRoot()
        assertEquals(900f, scroll.right.value - scroll.left.value, 1f)
    }
}
