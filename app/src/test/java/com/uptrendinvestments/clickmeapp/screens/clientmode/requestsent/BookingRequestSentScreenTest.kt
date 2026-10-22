package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentActions
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentDetailsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews.BookingRequestSentHeadline
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class BookingRequestSentScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val time12 = SimpleDateFormat("h:mm a", Locale.US)

    private val details =
        BookingRequestSentDetails(
            expertName = "Ada Lovelace",
            expertAvatarUrl = null,
            topicTitle = "Intro to ML",
            start = Instant.parse("2026-10-24T18:00:00Z"),
            end = Instant.parse("2026-10-24T19:00:00Z"),
            isFree = true,
        )

    private fun setCard(
        value: BookingRequestSentDetails = details,
        format: SimpleDateFormat = time12,
    ) {
        rule.setContent {
            ClickMeTheme {
                BookingRequestSentDetailsCard(details = value, zone = zone, timeFormat = format)
            }
        }
    }

    @Test
    fun headlineShowsHeadingAndBody() {
        rule.setContent { ClickMeTheme { BookingRequestSentHeadline(expertName = "Ada Lovelace") } }

        rule
            .onNodeWithText("Request Sent!")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule
            .onNodeWithText(
                "Your booking request has been sent to Ada Lovelace. They will review it and respond " +
                    "shortly. You'll be notified as soon as it's confirmed.",
            ).assertExists()
    }

    @Test
    fun detailsCardShowsRowsAndStatus() {
        setCard()

        rule.onNodeWithText("Booking Details").assertExists()
        rule.onNodeWithText("Expert").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Topic").assertExists()
        rule.onNodeWithText("Intro to ML").assertExists()
        rule.onNodeWithText("Date & Time").assertExists()
        rule.onNodeWithText("Oct 24, 2026 | 2:00 PM - 3:00 PM").assertExists()
        rule.onNodeWithText("Status").assertExists()
        rule.onNodeWithText("Awaiting Approval").assertExists()
    }

    @Test
    fun detailsCardNullExpertNameFallsBackToExpert() {
        setCard(details.copy(expertName = null))

        // The label and the fallback value both read "Expert".
        rule.onAllNodesWithText("Expert", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun detailsCardUses24HourDeviceFormat() {
        setCard(format = SimpleDateFormat("HH:mm", Locale.US))

        rule.onNodeWithText("Oct 24, 2026 | 14:00 - 15:00").assertExists()
    }

    @Test
    fun detailsCardHasNoPaymentWording() {
        setCard()

        rule.onAllNodesWithText("payment", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun actionsCallTheirCallbacksWhenEnabled() {
        var viewBookings = 0
        var backHome = 0
        rule.setContent {
            ClickMeTheme {
                BookingRequestSentActions(
                    onViewBookings = { viewBookings++ },
                    onBackToHome = { backHome++ },
                    enabled = true,
                )
            }
        }

        rule.onNodeWithText("View My Bookings").performClick()
        rule.onNodeWithText("Back to Home").performClick()

        assertEquals(1, viewBookings)
        assertEquals(1, backHome)
    }

    @Test
    fun actionsAreDisabledWhenNotEnabled() {
        rule.setContent {
            ClickMeTheme {
                BookingRequestSentActions(onViewBookings = {}, onBackToHome = {}, enabled = false)
            }
        }

        rule.onNodeWithText("View My Bookings").assertIsNotEnabled()
        rule.onNodeWithText("Back to Home").assertIsNotEnabled()
    }

    private fun setContent(
        state: BookingRequestSentUiState,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
        onViewBookings: () -> Unit = {},
        onBackToHome: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                BookingRequestSentContent(
                    state = state,
                    onBack = onBack,
                    onRetry = onRetry,
                    onViewBookings = onViewBookings,
                    onBackToHome = onBackToHome,
                    zone = zone,
                    timeFormat = time12,
                )
            }
        }
    }

    @Test
    fun loadingShowsTitleAndLoadingText() {
        setContent(BookingRequestSentUiState.Loading)

        rule.onNodeWithText("Booking Request").assertExists()
        rule.onNodeWithText("Loading your booking\u2026").assertExists()
    }

    @Test
    fun failedShowsFixedCopyAndRetryCallsOnRetry() {
        var retries = 0
        setContent(BookingRequestSentUiState.Failed, onRetry = { retries++ })

        rule.onNodeWithText("Couldn't load your booking").assertExists()
        rule
            .onNodeWithText(
                "Your request was sent, but we couldn't load the details. Check your connection and try again.",
            ).assertExists()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun loadedShowsContentAndWiresCallbacks() {
        var viewBookings = 0
        var backHome = 0
        var back = 0
        setContent(
            BookingRequestSentUiState.Loaded(details),
            onBack = { back++ },
            onViewBookings = { viewBookings++ },
            onBackToHome = { backHome++ },
        )
        rule.waitForIdle()

        rule.onNodeWithText("Request Sent!").assertExists()
        rule.onNodeWithContentDescription("Expert, Ada Lovelace").assertExists()
        rule.onNodeWithText("View My Bookings").assertIsEnabled()
        rule.onNodeWithText("Back to Home").assertIsEnabled()
        // The column scrolls and the test window is small, so bring each button into view first.
        rule.onNodeWithText("View My Bookings").performScrollTo().performClick()
        rule.onNodeWithText("Back to Home").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, viewBookings)
        assertEquals(1, backHome)
        assertEquals(1, back)
    }

    @Test
    fun actionsAreNotClickableUntilTheEntryAnimationEnds() {
        rule.mainClock.autoAdvance = false
        setContent(BookingRequestSentUiState.Loaded(details))
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("View My Bookings").assertIsNotEnabled()

        rule.mainClock.advanceTimeBy(800)

        rule.onNodeWithText("View My Bookings").assertIsEnabled()
        rule.onNodeWithText("Back to Home").assertIsEnabled()
    }

    @Test
    fun entryAnimationIsNotReplayedAfterRestoration() {
        val restorer = StateRestorationTester(rule)
        restorer.setContent {
            ClickMeTheme {
                BookingRequestSentContent(
                    state = BookingRequestSentUiState.Loaded(details),
                    onBack = {},
                    onRetry = {},
                    onViewBookings = {},
                    onBackToHome = {},
                    zone = zone,
                    timeFormat = time12,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("View My Bookings").assertIsEnabled()

        rule.mainClock.autoAdvance = false
        restorer.emulateSavedInstanceStateRestore()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("View My Bookings").assertIsEnabled()
    }

    @Test
    fun paidBookingRendersSameBlocksWithoutPaymentWording() {
        setContent(BookingRequestSentUiState.Loaded(details.copy(isFree = false)))
        rule.waitForIdle()

        rule.onNodeWithText("Request Sent!").assertExists()
        rule.onNodeWithText("Awaiting Approval").assertExists()
        rule.onAllNodesWithText("payment", substring = true, ignoreCase = true).assertCountEquals(0)
    }
}
