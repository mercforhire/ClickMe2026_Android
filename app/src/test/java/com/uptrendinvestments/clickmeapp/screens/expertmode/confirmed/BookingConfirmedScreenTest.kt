package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/** A context on which no activity can be started, like a device without a calendar app. */
private class NoCalendarContext(
    base: Context,
) : ContextWrapper(base) {
    override fun startActivity(intent: Intent?) = throw ActivityNotFoundException("no calendar")
}

@RunWith(RobolectricTestRunner::class)
class BookingConfirmedScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val time12 = SimpleDateFormat("h:mm a", Locale.US)

    private val key =
        BookingConfirmedKey(
            bookingId = "b1",
            clientName = "Ada Lovelace",
            clientAvatarUrl = null,
            topic = "Algorithms",
            startMillis = Instant.parse("2026-10-24T18:00:00Z").toEpochMilli(),
            endMillis = Instant.parse("2026-10-24T19:00:00Z").toEpochMilli(),
            meetingType = "skype_zoom",
            clientMessage = "See you",
        )

    private fun setContent(
        onBack: () -> Unit = {},
        onAddToCalendar: () -> Unit = {},
        onBackToHome: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                BookingConfirmedContent(
                    key = key,
                    onBack = onBack,
                    onAddToCalendar = onAddToCalendar,
                    onBackToHome = onBackToHome,
                    zone = zone,
                    timeFormat = time12,
                )
            }
        }
    }

    @Test
    fun showsHeadlineAndDetails() {
        setContent()
        rule.waitForIdle()

        rule
            .onNodeWithText("Booking Confirmed!")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule
            .onNodeWithText(
                "Your meeting with Ada Lovelace on Oct 24, 2026, at 2:00 PM has been successfully booked.",
            ).assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Oct 24, 2026, 2:00 PM (1 hour)").assertExists()
        rule.onNodeWithText("Skype/Zoom").assertExists()
        rule.onNodeWithText("Booking Confirmed").assertExists()
    }

    @Test
    fun buttonsEnabledAfterEntry() {
        rule.mainClock.autoAdvance = false
        setContent()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("confirmed_add_calendar").assertIsNotEnabled()
        rule.onNodeWithTag("confirmed_back_home").assertIsNotEnabled()

        rule.mainClock.advanceTimeBy(800)

        rule.onNodeWithTag("confirmed_add_calendar").assertIsEnabled()
        rule.onNodeWithTag("confirmed_back_home").assertIsEnabled()
    }

    @Test
    fun entryAnimationIsNotReplayedAfterRestoration() {
        val restorer = StateRestorationTester(rule)
        restorer.setContent {
            ClickMeTheme {
                BookingConfirmedContent(
                    key = key,
                    onBack = {},
                    onAddToCalendar = {},
                    onBackToHome = {},
                    zone = zone,
                    timeFormat = time12,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("confirmed_add_calendar").assertIsEnabled()

        rule.mainClock.autoAdvance = false
        restorer.emulateSavedInstanceStateRestore()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("confirmed_add_calendar").assertIsEnabled()
    }

    @Test
    fun backToHomeAndAddToCalendarCallTheirCallbacks() {
        var calendar = 0
        var home = 0
        var back = 0
        setContent(onBack = { back++ }, onAddToCalendar = { calendar++ }, onBackToHome = { home++ })
        rule.waitForIdle()

        rule.onNodeWithTag("confirmed_add_calendar").performScrollTo().performClick()
        rule.onNodeWithTag("confirmed_back_home").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, calendar)
        assertEquals(1, home)
        assertEquals(1, back)
    }

    @Test
    fun noCalendarAppShowsSnackbar() {
        rule.setContent {
            val base = LocalContext.current
            val noCalendarApp = remember { NoCalendarContext(base) }
            CompositionLocalProvider(LocalContext provides noCalendarApp) {
                ClickMeTheme {
                    BookingConfirmedScreen(key = key, onBack = {}, onBackToHome = {})
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("confirmed_add_calendar").performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("No calendar app found").assertExists()
    }
}
