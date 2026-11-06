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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_CHAT_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MINIMIZE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MUTE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_SPEAKER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_END_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ENDING_SOON_BANNER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_CHIP_TAG
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.navigationBarInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
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
 * BUG-22 and BUG-34 (Phase 16 matrix, D-09): the Minimize, Chat, Mute and Speaker controls must be reachable above the
 * pinned End Call.
 *
 * BUG-22: font 1.0, last minutes of the call (ending-soon banner and the time-left chip), the 360 dp glow box around the
 * hero left the controls hidden behind End Call without any scrolling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeetingCallScreenControlsReachableTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)
    private val navBar = 48

    private lateinit var view: View

    private fun state(
        connectionType: ConnectionType = ConnectionType.InAppVoice,
        status: CallStatus = CallStatus.Active,
        now: Instant = end.minusSeconds(120),
    ) = MeetingCallUiState(
        connectionType = connectionType,
        peerName = "Alex Doe",
        peerAvatarUrl = null,
        myAvatarUrl = null,
        topic = "Free Intro Chat",
        scheduledStart = start,
        scheduledEnd = end,
        status = status,
        timer = callTimerState(start, end, now),
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
        fontScale: Float,
        state: MeetingCallUiState,
    ) {
        rule.setContent {
            view = LocalView.current
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                ClickMeTheme {
                    Box(Modifier.size(width, height)) {
                        MeetingCallScreen(
                            state = state,
                            onMinimize = {},
                            onToggleMute = {},
                            onToggleSpeaker = {},
                            onEndCall = {},
                            onOpenExternal = {},
                            zone = ZoneOffset.UTC,
                            timeFormat = timeFormat,
                        )
                    }
                }
            }
        }
        rule.dispatchInsets(view, navigationBarInsets(navBar))
    }

    private fun assertControlsAboveEndCall(
        tags: List<String>,
        fontScale: Float,
    ) {
        val endTop = rule.onNodeWithTag(CALL_END_BUTTON_TAG).getUnclippedBoundsInRoot().top
        tags.forEach { tag ->
            rule.onNodeWithTag(tag).assertIsDisplayed()
            val bounds = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue(
                "$tag bottom is ${bounds.bottom} (top ${bounds.top}) but End Call starts at $endTop at font $fontScale",
                bounds.bottom <= endTop,
            )
        }
    }

    @Test
    fun controlsSitAboveEndCallWithTheBannerAndChipAtNormalFont() {
        show(360.dp, 800.dp, 1f, state())
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertExists()

        assertControlsAboveEndCall(ALL_CONTROLS, 1f)
    }

    private companion object {
        val ALL_CONTROLS =
            listOf(
                CALL_CONTROL_MINIMIZE_TAG,
                CALL_CONTROL_CHAT_TAG,
                CALL_CONTROL_MUTE_TAG,
                CALL_CONTROL_SPEAKER_TAG,
            )
    }
}
