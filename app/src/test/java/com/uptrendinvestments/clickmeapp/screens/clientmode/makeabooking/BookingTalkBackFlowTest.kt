package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.DetailsState
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.HeroRating
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ProfileDetailsUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ReviewsState
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.TopicRowUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.BOOK_SESSION_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.assertTraversalOrder
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
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
 * SC-4 flow 2 (Expert Profile, Make a Booking) semantics: the TalkBack names, roles and states of the entry and
 * submit controls (Phase 15 UI-SPEC "Flow 2: Book a session"). JVM proof of exposure, not of TalkBack speech.
 *
 * The window is 411 dp wide and tall enough for the whole form: the semantics bounds of nodes scrolled out of view are
 * clipped to an empty rect, which would break the order and target-size checks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class BookingTalkBackFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private val slot = Instant.parse("2026-10-22T20:00:00Z")
    private var bookCount = 0

    private fun stateIs(expected: String) =
        SemanticsMatcher("stateDescription == $expected") {
            it.config.getOrNull(SemanticsProperties.StateDescription) == expected
        }

    private fun profileState() =
        ExpertProfileUiState(
            expertId = "e1",
            name = "Ada Lovelace",
            title = "Engineer",
            imageUrl = null,
            tags = listOf("AI"),
            rating = HeroRating.Shown(4.8, 7),
            details =
                DetailsState.Loaded(
                    ProfileDetailsUi(
                        experienceYears = 5,
                        totalBookings = 3,
                        bio = "Pioneer of computing",
                        topics = listOf(TopicRowUi("t1", "Topic one", 30, null, PriceDisplay.Free)),
                        isBookable = true,
                    ),
                ),
            reviews = ReviewsState.Loaded(emptyList()),
        )

    private fun bookingState(
        selectedSlotStart: Instant? = slot,
        isSubmitting: Boolean = false,
    ): MakeABookingUiState {
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
            selectedSlotStart = selectedSlotStart,
            meetingType = MeetingType.InAppVoice,
            isSubmitting = isSubmitting,
        )
    }

    private fun setBooking(state: MakeABookingUiState) {
        rule.setContent {
            ClickMeTheme {
                MakeABookingContent(
                    state = state,
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
                    onBook = { bookCount++ },
                    onOpenPaymentMethods = {},
                    onDismissError = {},
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
    }

    @Test
    fun bookSessionNamesExpert() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = profileState(),
                    onBack = {},
                    onBookSession = {},
                    onRetry = {},
                )
            }
        }

        rule
            .onNodeWithTag(BOOK_SESSION_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Book Session with Ada Lovelace"),
                ),
            )
        rule.assertTraversalOrder("Ada Lovelace", "Book Session with Ada Lovelace")
    }

    @Test
    fun bookNowDisabledReason() {
        setBooking(bookingState(selectedSlotStart = null))

        rule
            .onNodeWithTag(BOOK_NOW_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
            .assert(stateIs("Choose a time slot to continue"))
    }

    @Test
    fun bookNowBusy() {
        setBooking(bookingState(isSubmitting = true))

        rule
            .onNodeWithTag(BOOK_NOW_TAG)
            .performScrollTo()
            .assert(stateIs("In progress"))
            .performClick()
        assertEquals(0, bookCount)
    }

    @Test
    fun formNamedAndLargeEnough() {
        setBooking(bookingState())

        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun formOrder() {
        setBooking(bookingState())

        rule.assertTraversalOrder(
            "Book a Session",
            "Topic of Discussion",
            "October 2026",
            "4:00 PM",
            "In-app Voice Call",
            "Message (optional)",
            "Book Now",
        )
    }
}
