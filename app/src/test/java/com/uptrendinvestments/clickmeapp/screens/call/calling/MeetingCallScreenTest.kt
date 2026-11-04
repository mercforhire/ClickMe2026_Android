package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.chat.MESSAGE_BUTTON_SPINNER_TAG
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_CHAT_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MINIMIZE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MUTE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_SPEAKER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_END_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ENDING_SOON_BANNER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.EXTERNAL_COPY_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.EXTERNAL_OPEN_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_HERO_PAIR_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_HERO_SINGLE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_NAME_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_STATUS_TAG
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

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeetingCallScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)

    private fun inApp(
        status: CallStatus = CallStatus.Connecting,
        remaining: Long = 1500,
    ) = MeetingCallUiState(
        connectionType = ConnectionType.InAppVoice,
        peerName = "Alex Doe",
        peerAvatarUrl = null,
        myAvatarUrl = null,
        topic = "Career advice",
        scheduledStart = start,
        scheduledEnd = end,
        status = status,
        timer = callTimerState(start, end, end.minusSeconds(remaining)),
        isMuted = false,
        isSpeakerOn = true,
        isEndingCall = false,
        externalJoinUrl = null,
        provider = ExternalMeetingProvider.Unknown,
        hasOpenedExternalLink = false,
    )

    private fun external(
        remaining: Long = 1500,
        opened: Boolean = false,
    ) = inApp(remaining = remaining).copy(
        connectionType = ConnectionType.SkypeZoom,
        externalJoinUrl = "https://zoom.us/j/123",
        provider = ExternalMeetingProvider.Zoom,
        hasOpenedExternalLink = opened,
    )

    private fun show(
        state: MeetingCallUiState,
        onMinimize: () -> Unit = {},
        onToggleMute: () -> Unit = {},
        onToggleSpeaker: () -> Unit = {},
        onEndCall: () -> Unit = {},
        onOpenExternal: () -> Unit = {},
        onChat: () -> Unit = {},
        chatBusy: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                MeetingCallScreen(
                    state = state,
                    onMinimize = onMinimize,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onEndCall = onEndCall,
                    onOpenExternal = onOpenExternal,
                    chatBusy = chatBusy,
                    onChat = onChat,
                    zone = ZoneOffset.UTC,
                    timeFormat = timeFormat,
                )
            }
        }
    }

    @Test
    fun inAppConnectingShowsSingleHeroStatusControlsAndEndCall() {
        show(inApp(CallStatus.Connecting))
        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertDoesNotExist()
        rule.onNodeWithTag(MEETING_CALL_STATUS_TAG).assertExists()
        rule.onNodeWithText("Connecting…").assertExists()
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertExists().assertIsNotEnabled()
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assertExists()
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertExists()
        rule.onNodeWithText("End Call").assertExists()
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).assertDoesNotExist()
        rule.onNodeWithTag(EXTERNAL_COPY_TAG).assertDoesNotExist()
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun inAppActiveShowsThePairHeroAndConnected() {
        show(inApp(CallStatus.Active))
        rule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertExists()
        rule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertDoesNotExist()
        rule.onNodeWithText("Connected").assertExists()
    }

    @Test
    fun externalVariantShowsOpenCopyStatusAndEndSessionWithoutAudioControls() {
        show(external())
        rule.onNodeWithText("Open in Zoom").assertExists()
        rule.onNodeWithText("Copy Link").assertExists()
        rule.onNodeWithText("Open the meeting link to start the session.").assertExists()
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assertDoesNotExist()
        rule.onNodeWithText("End Session").assertExists()
        rule.onNodeWithText("Connected").assertDoesNotExist()
        rule.onNodeWithTag(MEETING_CALL_STATUS_TAG).assertDoesNotExist()
    }

    @Test
    fun finalStretchShowsTheEndingSoonBannerInBothVariants() {
        show(inApp(remaining = 120))
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
    }

    @Test
    fun finalStretchShowsTheEndingSoonBannerInTheExternalVariant() {
        show(external(remaining = 120))
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertExists()
    }

    @Test
    fun inAppCallbacksFire() {
        var minimize = 0
        var mute = 0
        var speaker = 0
        var endCall = 0
        show(
            inApp(),
            onMinimize = { minimize++ },
            onToggleMute = { mute++ },
            onToggleSpeaker = { speaker++ },
            onEndCall = { endCall++ },
        )
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).performClick()
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).performClick()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).performClick()
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).performClick()
        assertEquals(listOf(1, 1, 1, 1), listOf(minimize, mute, speaker, endCall))
    }

    @Test
    fun externalCallbacksFire() {
        var open = 0
        var endCall = 0
        var minimize = 0
        show(external(), onMinimize = { minimize++ }, onEndCall = { endCall++ }, onOpenExternal = { open++ })
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).performClick()
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).performClick()
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).performClick()
        assertEquals(listOf(1, 1, 1), listOf(open, minimize, endCall))
    }

    @Test
    fun externalLayoutChatEnabledWithAPeerAndCallsOnChat() {
        var chats = 0
        show(external().copy(peerId = "p1"), onChat = { chats++ })
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertIsEnabled().performClick()
        assertEquals(1, chats)
    }

    @Test
    fun externalLayoutChatIsNotEnabledWithoutAPeer() {
        show(external().copy(peerId = null))
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertIsNotEnabled()
    }

    @Test
    fun inAppLayoutChatBusyShowsTheSpinnerAndIgnoresTaps() {
        var chats = 0
        show(inApp().copy(peerId = "p1"), onChat = { chats++ }, chatBusy = true)
        rule.onNodeWithTag(MESSAGE_BUTTON_SPINNER_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertIsEnabled().performClick()
        assertEquals(0, chats)
    }

    @Test
    fun endButtonStaysReachableAtTwoHundredPercentFontScaleOnASmallWindow() {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                ClickMeTheme {
                    Box(Modifier.size(320.dp, 640.dp)) {
                        MeetingCallScreen(
                            state = inApp(remaining = 120),
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
        // BUG-10: End Call is pinned below the scrolling content, so it is displayed without scrolling.
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun rootIsATraversalGroupAndTheNameIsAHeading() {
        show(inApp())
        rule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            .onFirst()
            .assertExists()
        rule
            .onNodeWithTag(MEETING_CALL_NAME_TAG)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    @Config(qualifiers = "w900dp-h400dp")
    fun controlsVisibleOnShortWindow() {
        show(inApp())

        listOf(CALL_CONTROL_MUTE_TAG, CALL_CONTROL_SPEAKER_TAG, CALL_END_BUTTON_TAG).forEach { tag ->
            rule.onNodeWithTag(tag).assertIsDisplayed()
            val bottom = rule.onNodeWithTag(tag).getBoundsInRoot().bottom
            assertTrue("$tag bottom is $bottom, expected <= 400.dp without scrolling", bottom <= 400.dp)
        }
        rule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertHeightIsAtLeast(96.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun avatarUnchangedOnPhone() {
        show(inApp())

        // 160 dp avatar + 2 * (4 dp ring + 8 dp gap).
        rule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertHeightIsEqualTo(184.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun longPeerNameTwoLines() {
        show(inApp().copy(peerName = "Alexandria ".repeat(11).trim()))

        val results = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithTag(MEETING_CALL_NAME_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        assertTrue("name lines: ${results.map { it.lineCount }}", results.single().lineCount <= 2)
        listOf(CALL_CONTROL_MUTE_TAG, CALL_CONTROL_SPEAKER_TAG).forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertIsDisplayed()
    }
}
