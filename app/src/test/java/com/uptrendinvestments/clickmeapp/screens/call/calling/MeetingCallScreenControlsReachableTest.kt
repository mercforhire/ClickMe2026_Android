package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_HERO_PAIR_TAG
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
    private var navBar = 48

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

    /** The pre-join Waiting state of BUG-34: the countdown to the start, no banner. */
    private fun waiting() = state(status = CallStatus.WaitingForPeer, now = start.minusSeconds(3100))

    private fun showInOverlay(
        fontScale: Float,
        state: MeetingCallUiState,
    ) {
        rule.setContent {
            view = LocalView.current
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                ClickMeTheme {
                    CallOverlayContent(
                        state = state,
                        isMinimized = false,
                        dialog = null,
                        pendingPermission = null,
                        onMinimize = {},
                        onExpand = {},
                        onToggleMute = {},
                        onToggleSpeaker = {},
                        onEndCall = {},
                        onOpenExternal = {},
                        onDismissDialog = {},
                        onPermissionResult = { _, _ -> },
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        rule.dispatchInsets(view, navigationBarInsets(navBar))
        rule.waitForIdle()
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertExists()
    }

    /** Swipes the call content like a finger, at most [maxSwipes] times, until every control is above End Call. */
    private fun swipeUntilControlsAboveEndCall(
        fontScale: Float,
        maxSwipes: Int = 3,
    ) {
        // Content that already fits above End Call has nothing to scroll: a swipe is only required to move it when
        // some control is still below End Call.
        val muteTop = { rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).getUnclippedBoundsInRoot().top }
        val needsScroll = !controlsAboveEndCall(ALL_CONTROLS)
        val before = muteTop()
        rule.onNodeWithTag(CALL_SCROLL_CONTENT_TAG).performTouchInput { swipeUp() }
        rule.waitForIdle()
        val after = muteTop()
        assertTrue(
            "A swipe up did not move the content: Mute top stayed $before (now $after)",
            !needsScroll || after < before,
        )
        var swipes = 1
        while (swipes < maxSwipes && !controlsAboveEndCall(ALL_CONTROLS)) {
            rule.onNodeWithTag(CALL_SCROLL_CONTENT_TAG).performTouchInput { swipeUp() }
            rule.waitForIdle()
            swipes++
        }
        assertControlsAboveEndCall(ALL_CONTROLS, fontScale)
    }

    private fun controlsAboveEndCall(tags: List<String>): Boolean {
        val endTop = rule.onNodeWithTag(CALL_END_BUTTON_TAG).getUnclippedBoundsInRoot().top
        return tags.all { rule.onNodeWithTag(it).getUnclippedBoundsInRoot().bottom <= endTop }
    }

    @Test
    fun controlsSitAboveEndCallWithTheBannerAndChipAtNormalFont() {
        show(360.dp, 800.dp, 1f, state())
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertExists()

        assertControlsAboveEndCall(ALL_CONTROLS, 1f)
    }

    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun controlsReachableByScrollAtTwoHundredPercentInTheWaitingState() {
        navBar = BUG_34_NAV_BAR_PX
        show(339.dp, 753.dp, 2f, waiting())

        ALL_CONTROLS.forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo()
            assertControlsAboveEndCall(listOf(tag), 2f)
        }
    }

    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun aSwipeUpScrollsTheCallContentAtTwoHundredPercent() {
        navBar = BUG_34_NAV_BAR_PX
        show(339.dp, 753.dp, 2f, waiting())

        swipeUntilControlsAboveEndCall(2f)
    }

    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun aSwipeUpScrollsTheCallContentInsideTheOverlayAtTwoHundredPercent() {
        navBar = BUG_34_NAV_BAR_PX
        showInOverlay(2f, waiting())

        swipeUntilControlsAboveEndCall(2f)
    }

    /**
     * BUG-34 on the device: the controls start below End Call and a swipe did not bring them up. The Robolectric
     * swipe scrolls even on the old layout (the no-scroll cause could not be reproduced on the JVM), so this pins the
     * part that is reproducible: with the glow out of layout the controls fit above End Call at rest, a 200% font and
     * the largest display zoom included, with no need to scroll at all.
     */
    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun controlsFitAboveEndCallAtRestAtTwoHundredPercentInTheWaitingState() {
        navBar = BUG_34_NAV_BAR_PX
        show(339.dp, 753.dp, 2f, waiting())

        assertControlsAboveEndCall(ALL_CONTROLS, 2f)
    }

    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun externalVariantControlsReachableAtTwoHundredPercentWithTheBanner() {
        navBar = BUG_34_NAV_BAR_PX
        show(339.dp, 753.dp, 2f, state(connectionType = ConnectionType.SkypeZoom))
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()

        // The external variant has no Mute or Speaker.
        listOf(CALL_CONTROL_MINIMIZE_TAG, CALL_CONTROL_CHAT_TAG).forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo()
            assertControlsAboveEndCall(listOf(tag), 2f)
        }
    }

    @Test
    @Config(qualifiers = BUG_34_WINDOW)
    fun connectedPairControlsReachableAtTwoHundredPercentWithTheBanner() {
        navBar = BUG_34_NAV_BAR_PX
        show(339.dp, 753.dp, 2f, state(status = CallStatus.Active))
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertExists()

        ALL_CONTROLS.forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo()
            assertControlsAboveEndCall(listOf(tag), 2f)
        }
    }

    private companion object {
        /** 720x1600 px at 340 dpi with the largest display zoom: 339x753 dp (BUG-34 device). */
        const val BUG_34_WINDOW = "w339dp-h753dp-340dpi"

        /** The 3-button navigation bar on the BUG-34 device is about 100 px (47 dp) tall (FS.C3-200pct.png). */
        const val BUG_34_NAV_BAR_PX = 100

        val ALL_CONTROLS =
            listOf(
                CALL_CONTROL_MINIMIZE_TAG,
                CALL_CONTROL_CHAT_TAG,
                CALL_CONTROL_MUTE_TAG,
                CALL_CONTROL_SPEAKER_TAG,
            )
    }
}
