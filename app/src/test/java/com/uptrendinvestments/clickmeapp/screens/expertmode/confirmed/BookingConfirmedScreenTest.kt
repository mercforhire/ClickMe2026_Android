package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.uptrendinvestments.clickmeapp.screens.expertmode.DashboardLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
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
import java.time.ZoneId
import java.util.Locale

/** A context on which no activity can be started, like a device without a calendar app. */
private class NoCalendarContext(
    base: Context,
) : ContextWrapper(base) {
    override fun startActivity(intent: Intent?) = throw ActivityNotFoundException("no calendar")
}

/** The system "animations off" setting as the composition sees it: every animation jumps to its end. */
private object MotionOff : MotionDurationScale {
    override val scaleFactor: Float = 0f
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
        val viewModel = BookingConfirmedViewModel(ExpertLaunchRequest())
        rule.setContent {
            val base = LocalContext.current
            val noCalendarApp = remember { NoCalendarContext(base) }
            CompositionLocalProvider(LocalContext provides noCalendarApp) {
                ClickMeTheme {
                    BookingConfirmedScreen(
                        key = key,
                        onBack = {},
                        onBackToHome = {},
                        viewModel = viewModel,
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("confirmed_add_calendar").performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("No calendar app found").assertExists()
    }

    @Test
    fun headlineFocusedOnEntry() {
        setContent()
        rule.waitForIdle()

        rule
            .onNodeWithText("Booking Confirmed!")
            .assertIsFocused()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun headlineFocusedAgainOnRecreation() {
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
        rule.onNodeWithText("Booking Confirmed!").assertIsFocused()

        restorer.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // A recreated screen is a new entry, so the headline takes focus again and the buttons are already enabled.
        rule.onNodeWithText("Booking Confirmed!").assertIsFocused()
        rule.onNodeWithTag("confirmed_add_calendar").assertIsEnabled()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        setContent()
        rule.waitForIdle()

        val content = rule.onNodeWithTag(BOOKING_CONFIRMED_CONTENT_TAG).getUnclippedBoundsInRoot()
        val width = content.right.value - content.left.value
        assertTrue("Confirmed content is $width dp wide, expected <= 600", width <= 600f)
        val button = rule.onNodeWithTag("confirmed_add_calendar").getUnclippedBoundsInRoot()
        val buttonWidth = button.right.value - button.left.value
        assertTrue("Add to Calendar is $buttonWidth dp wide, expected <= 600", buttonWidth <= 600f)
    }

    @Test
    fun backToHomePostsDashboardReload() {
        val launchRequest = ExpertLaunchRequest()
        var home = 0
        var pendingWhenNavigating: DashboardLaunch? = null
        val viewModel = BookingConfirmedViewModel(launchRequest)
        rule.setContent {
            ClickMeTheme {
                BookingConfirmedScreen(
                    key = key,
                    onBack = {},
                    onBackToHome = {
                        home++
                        pendingWhenNavigating = launchRequest.dashboard.pending.value
                    },
                    viewModel = viewModel,
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("confirmed_back_home").performScrollTo().performClick()

        assertEquals(1, home)
        // The reload is posted before the navigation callback runs and stays pending for the Dashboard.
        assertEquals(DashboardLaunch, pendingWhenNavigating)
        assertEquals(DashboardLaunch, launchRequest.dashboard.consume())
    }
}

/** Own class: `runComposeUiTest` must not share a JVM test with a `createComposeRule` rule. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class BookingConfirmedMotionTest {
    @Test
    fun animationsOffShowsFinalState() =
        runComposeUiTest(effectContext = MotionOff) {
            mainClock.autoAdvance = false
            setContent {
                ClickMeTheme {
                    BookingConfirmedContent(
                        key =
                            BookingConfirmedKey(
                                bookingId = "b1",
                                clientName = "Ada Lovelace",
                                clientAvatarUrl = null,
                                topic = "Algorithms",
                                startMillis = Instant.parse("2026-10-24T18:00:00Z").toEpochMilli(),
                                endMillis = Instant.parse("2026-10-24T19:00:00Z").toEpochMilli(),
                                meetingType = "skype_zoom",
                                clientMessage = null,
                            ),
                        onBack = {},
                        onAddToCalendar = {},
                        onBackToHome = {},
                        zone = ZoneId.of("America/Toronto"),
                        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    )
                }
            }
            // With animations on the buttons stay disabled until the last block lands (buttonsEnabledAfterEntry).
            mainClock.advanceTimeBy(100)

            onNodeWithTag("confirmed_add_calendar").assertIsEnabled()
            onNodeWithTag("confirmed_back_home").assertIsEnabled()
        }
}
