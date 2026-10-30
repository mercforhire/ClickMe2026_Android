package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MUTE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_SPEAKER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_NAME_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_STATUS_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_TIMER_TAG
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.assertTraversalOrder
import com.uptrendinvestments.clickmeapp.testing.traversalLabels
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
 * SC-4 flow 3 (join a call), call-layer half: the order TalkBack walks the expanded layer in, the live-region rules and
 * the focus move on entry (UI-SPEC Accessibility Contract "Flow 3", POL-04). JVM semantics only; whether TalkBack
 * actually speaks this order is a device-checklist row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CallTalkBackFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)

    private var state by mutableStateOf(connectedState())

    private fun connectedState(status: CallStatus = CallStatus.Active) =
        MeetingCallUiState(
            connectionType = ConnectionType.InAppVoice,
            peerName = "Ada",
            peerAvatarUrl = null,
            myAvatarUrl = null,
            topic = "Career advice",
            scheduledStart = start,
            scheduledEnd = end,
            status = status,
            timer = callTimerState(start, end, start.plusSeconds(300)),
            isMuted = false,
            isSpeakerOn = true,
            isEndingCall = false,
            externalJoinUrl = null,
            provider = ExternalMeetingProvider.Unknown,
            hasOpenedExternalLink = false,
            peerId = "p1",
        )

    private fun show() {
        rule.setContent {
            ClickMeTheme {
                MeetingCallScreen(
                    state = state,
                    onMinimize = {},
                    onToggleMute = { state = state.copy(isMuted = !state.isMuted) },
                    onToggleSpeaker = { state = state.copy(isSpeakerOn = !state.isSpeakerOn) },
                    onEndCall = {},
                    onOpenExternal = {},
                    zone = ZoneOffset.UTC,
                    timeFormat = timeFormat,
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun layerReadingOrder() {
        show()

        rule.assertTraversalOrder("Connected", "Ada", "elapsed", "Mute", "Speaker", "Chat", "Minimize call", "End Call")
    }

    @Test
    fun endCallIsLast() {
        show()

        assertTrue(rule.traversalLabels().last().contains("End Call"))
    }

    @Test
    fun statusIsPoliteLiveRegionAndTimerIsNot() {
        show()

        rule
            .onNodeWithTag(MEETING_CALL_STATUS_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule
            .onNodeWithTag(
                MEETING_CALL_TIMER_TAG,
            ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithTag(MEETING_CALL_NAME_TAG).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun statusFocusedOnEntry() {
        show()

        rule.onNodeWithTag(MEETING_CALL_STATUS_TAG).assertIsFocused()
    }

    @Test
    fun switchesExposeState() {
        show()
        val role = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assert(role)
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assert(stateIs("Unmuted"))
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assert(role)
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assert(stateIs("On"))

        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).performClick()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assert(stateIs("Muted"))
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assert(stateIs("Off"))
    }

    @Test
    fun controlsNamedAndLargeEnough() {
        show()

        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    private fun stateIs(expected: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)
}
