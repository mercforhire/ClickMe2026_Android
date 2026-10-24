package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.components.avatar.AVATAR_FALLBACK_TAG
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MeetingCallHeroTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = Instant.parse("2026-10-24T10:30:00Z")

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent { ClickMeTheme { content() } }
    }

    private fun info(
        peerName: String? = "Ada Lovelace",
        topic: String? = "Algorithms",
        scheduledEnd: Instant? = end,
        timer: CallTimerState = CallTimerState(elapsedSeconds = 125, remainingSeconds = 1675),
        statusText: String? = "Connected",
    ) = setContent {
        MeetingCallInfo(
            peerName = peerName,
            topic = topic,
            scheduledStart = start,
            scheduledEnd = scheduledEnd,
            timer = timer,
            statusText = statusText,
            zone = ZoneOffset.UTC,
            timeFormat = SimpleDateFormat("h:mm a", Locale.US),
        )
    }

    @Test
    fun connectingShowsSingleHeroOnly() {
        setContent { MeetingCallAvatar(CallStatus.Connecting, "https://x/p.png", null) }
        composeRule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertExists()
        composeRule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertDoesNotExist()
        composeRule.onAllNodesWithTag(AVATAR_FALLBACK_TAG).assertCountEquals(1)
    }

    @Test
    fun waitingForPeerAndEndedShowSingleHero() {
        setContent { MeetingCallAvatar(CallStatus.WaitingForPeer, null, null) }
        composeRule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertExists()
    }

    @Test
    fun endedShowsSingleHero() {
        setContent { MeetingCallAvatar(CallStatus.Ended, null, null) }
        composeRule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertExists()
        composeRule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertDoesNotExist()
    }

    @Test
    fun activeShowsPairWithTwoAvatars() {
        setContent { MeetingCallAvatar(CallStatus.Active, null, null) }
        composeRule.onNodeWithTag(MEETING_CALL_HERO_PAIR_TAG).assertExists()
        composeRule.onNodeWithTag(MEETING_CALL_HERO_SINGLE_TAG).assertDoesNotExist()
        composeRule.onAllNodesWithTag(AVATAR_FALLBACK_TAG).assertCountEquals(2)
    }

    @Test
    fun avatarsAreDecorative() {
        setContent { MeetingCallAvatar(CallStatus.Active, null, null) }
        val describedNodes =
            composeRule
                .onAllNodesWithTag(AVATAR_FALLBACK_TAG)
                .fetchSemanticsNodes()
                .count { SemanticsProperties.ContentDescription in it.config }
        assertEquals(0, describedNodes)
    }

    @Test
    fun infoShowsNameTopicRangeTimerAndStatus() {
        info()
        composeRule
            .onNodeWithTag(MEETING_CALL_NAME_TAG)
            .assertTextEquals("Ada Lovelace")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText("Algorithms").assertIsDisplayed()
        composeRule.onNodeWithText("10:00 AM – 10:30 AM").assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_CALL_TIMER_TAG).assertTextEquals("02:05")
        composeRule.onNodeWithTag(MEETING_CALL_STATUS_TAG).assertTextEquals("Connected")
        composeRule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertDoesNotExist()
    }

    @Test
    fun chipShowsInFinalStretchOnly() {
        info(timer = CallTimerState(elapsedSeconds = 1500, remainingSeconds = 300))
        composeRule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertTextEquals("5:00 left")
    }

    @Test
    fun chipHiddenAtZeroRemaining() {
        info(timer = CallTimerState(elapsedSeconds = 1800, remainingSeconds = 0))
        composeRule.onNodeWithTag(MEETING_CALL_CHIP_TAG).assertDoesNotExist()
    }

    @Test
    fun blankTopicAndNullEndHideTheirLines() {
        info(topic = " ", scheduledEnd = null)
        composeRule.onNodeWithText("Algorithms").assertDoesNotExist()
        composeRule.onNodeWithText("10:00 AM – 10:30 AM").assertDoesNotExist()
    }

    @Test
    fun nullNameFallsBackToGenericTitle() {
        info(peerName = null)
        composeRule.onNodeWithTag(MEETING_CALL_NAME_TAG).assertTextEquals("ClickMe call")
    }

    @Test
    fun statusIsPolitelyAnnouncedAndTimerIsNotALiveRegion() {
        info()
        composeRule
            .onNodeWithTag(MEETING_CALL_STATUS_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule
            .onNodeWithTag(MEETING_CALL_TIMER_TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
            .assertContentDescriptionEquals("2 minutes 5 seconds elapsed")
    }

    @Test
    fun nullStatusTextHidesTheStatusLine() {
        info(statusText = null)
        composeRule.onNodeWithTag(MEETING_CALL_STATUS_TAG).assertDoesNotExist()
    }

    @Test
    fun callStatusTextMapsEveryStatus() {
        val seen = mutableMapOf<CallStatus, String>()
        setContent { CallStatus.entries.forEach { seen[it] = callStatusText(it) } }
        composeRule.waitForIdle()
        assertEquals("Connecting…", seen[CallStatus.Connecting])
        assertEquals("Waiting for other person…", seen[CallStatus.WaitingForPeer])
        assertEquals("Connected", seen[CallStatus.Active])
        assertEquals("Call Ended", seen[CallStatus.Ended])
    }

    @Test
    fun externalHeroShowsNameTopicAndRange() {
        setContent {
            ExternalSessionHero(
                peerName = "Ada Lovelace",
                peerAvatarUrl = null,
                topic = "Algorithms",
                scheduledStart = start,
                scheduledEnd = end,
                zone = ZoneOffset.UTC,
                timeFormat = SimpleDateFormat("h:mm a", Locale.US),
            )
        }
        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText("Algorithms").assertIsDisplayed()
        composeRule.onNodeWithText("10:00 AM – 10:30 AM").assertIsDisplayed()
        composeRule.onAllNodesWithTag(AVATAR_FALLBACK_TAG).assertCountEquals(1)
    }
}
