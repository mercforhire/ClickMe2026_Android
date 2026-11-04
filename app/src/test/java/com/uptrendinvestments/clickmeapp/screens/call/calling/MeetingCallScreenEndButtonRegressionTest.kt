package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_END_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ENDING_SOON_BANNER_TAG
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.navigationBarInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * BUG-10: with the ending-soon banner and the time-left chip both showing, the call content overflowed its column, End
 * Call was cut off at the bottom and the navigation bar drew over it until the user scrolled. End Call is pinned below
 * the scrolling content, so it stays on screen and above the navigation bar however tall the content is.
 *
 * The `w360dp-h800dp` qualifier gives 1 px = 1 dp, so the inset below is 48 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeetingCallScreenEndButtonRegressionTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)
    private val navBar = 48

    private lateinit var view: View
    private var endCalls = 0

    private fun state(connectionType: ConnectionType = ConnectionType.InAppVoice) =
        MeetingCallUiState(
            connectionType = connectionType,
            peerName = "Alex Doe",
            peerAvatarUrl = null,
            myAvatarUrl = null,
            topic = "Free Intro Chat",
            scheduledStart = start,
            scheduledEnd = end,
            status = CallStatus.Active,
            timer = callTimerState(start, end, end.minusSeconds(120)),
            isMuted = false,
            isSpeakerOn = true,
            isEndingCall = false,
            externalJoinUrl = if (connectionType == ConnectionType.SkypeZoom) "https://zoom.us/j/123" else null,
            provider = ExternalMeetingProvider.Zoom,
            hasOpenedExternalLink = false,
            peerId = "p1",
        )

    private fun show(
        width: Dp,
        height: Dp,
        fontScale: Float = 1f,
        connectionType: ConnectionType = ConnectionType.InAppVoice,
    ) {
        rule.setContent {
            view = LocalView.current
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                ClickMeTheme {
                    Box(Modifier.size(width, height)) {
                        MeetingCallScreen(
                            state = state(connectionType),
                            onMinimize = {},
                            onToggleMute = {},
                            onToggleSpeaker = {},
                            onEndCall = { endCalls++ },
                            onOpenExternal = {},
                            zone = ZoneOffset.UTC,
                            timeFormat = timeFormat,
                        )
                    }
                }
            }
        }
        rule.dispatchInsets(view, navigationBarInsets(navBar))
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
    }

    private fun assertEndCallClearOfTheNavigationBar(windowHeight: Dp) {
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertIsDisplayed()
        val bottom = rule.onNodeWithTag(CALL_END_BUTTON_TAG).getUnclippedBoundsInRoot().bottom
        val limit = windowHeight - navBar.dp
        assertTrue("End Call bottom is $bottom, the navigation bar starts at $limit", bottom <= limit)
    }

    @Test
    fun endCallStaysAboveTheNavigationBarWhenTheBannerAndChipShow() {
        show(360.dp, 640.dp)

        assertEndCallClearOfTheNavigationBar(640.dp)
    }

    @Test
    fun endCallStaysAboveTheNavigationBarOnTheTestPhoneWindow() {
        show(360.dp, 800.dp)

        assertEndCallClearOfTheNavigationBar(800.dp)
    }

    @Test
    fun endCallStaysOnScreenAtTwoHundredPercentFontScale() {
        show(320.dp, 640.dp, fontScale = 2f)

        assertEndCallClearOfTheNavigationBar(640.dp)
    }

    @Test
    fun endSessionStaysAboveTheNavigationBarInTheExternalVariant() {
        show(360.dp, 640.dp, connectionType = ConnectionType.SkypeZoom)

        assertEndCallClearOfTheNavigationBar(640.dp)
    }

    @Test
    fun endCallCanBeTappedWithoutScrolling() {
        show(360.dp, 640.dp)

        rule.onNodeWithTag(CALL_END_BUTTON_TAG).performClick()

        assertEquals(1, endCalls)
    }
}
