package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeetingCallActionsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun controls(
        showAudioControls: Boolean = true,
        isMuted: Boolean = false,
        isSpeakerOn: Boolean = true,
        onMinimize: () -> Unit = {},
        onToggleMute: () -> Unit = {},
        onToggleSpeaker: () -> Unit = {},
    ) = show {
        MeetingCallControls(
            showAudioControls = showAudioControls,
            isMuted = isMuted,
            isSpeakerOn = isSpeakerOn,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            onToggleSpeaker = onToggleSpeaker,
        )
    }

    // ---- EndingSoonBanner ----

    @Test
    fun bannerShowsTheWrapUpCopyInTheLastFiveMinutesAsAPoliteLiveRegion() {
        show { EndingSoonBanner(CallTimerState(elapsedSeconds = 1500, remainingSeconds = 300)) }
        rule.onNodeWithText("Session ending soon — please start wrapping up.").assertExists()
        rule
            .onNodeWithTag(ENDING_SOON_BANNER_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun bannerShowsTheTimeCompleteCopyAfterTheScheduledEnd() {
        show { EndingSoonBanner(CallTimerState(elapsedSeconds = 1800, remainingSeconds = 0)) }
        rule.onNodeWithText("Scheduled time complete — you can wrap up when ready.").assertExists()
    }

    @Test
    fun bannerIsHiddenOutsideTheFinalStretchOrWithoutAnEnd() {
        show { EndingSoonBanner(CallTimerState(elapsedSeconds = 1000, remainingSeconds = 301)) }
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun bannerIsHiddenWhenTheEndIsUnknown() {
        show { EndingSoonBanner(CallTimerState(elapsedSeconds = 1000, remainingSeconds = null)) }
        rule.onNodeWithTag(ENDING_SOON_BANNER_TAG).assertDoesNotExist()
    }

    // ---- MeetingCallControls ----

    @Test
    fun inAppControlsShowFourControlsWithTheirLabels() {
        controls()
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assertExists()
        rule.onNodeWithText("Minimize").assertExists()
        rule.onNodeWithText("Chat").assertExists()
        rule.onNodeWithText("Mute").assertExists()
        rule.onNodeWithText("Speaker").assertExists()
    }

    @Test
    fun minimizeIsAnnouncedAsMinimizeCallAndCallsBack() {
        var minimized = 0
        controls(onMinimize = { minimized++ })
        rule
            .onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Minimize call")))
            .performClick()
        assertEquals(1, minimized)
    }

    @Test
    fun chatIsDisabledWithAvailableSoon() {
        controls()
        rule
            .onNodeWithTag(CALL_CONTROL_CHAT_TAG)
            .assertIsNotEnabled()
            .assert(hasStateDescription("Available soon"))
    }

    @Test
    fun muteReportsUnmutedAndTogglesOnClick() {
        var toggles = 0
        controls(isMuted = false, onToggleMute = { toggles++ })
        rule
            .onNodeWithTag(CALL_CONTROL_MUTE_TAG)
            .assertIsEnabled()
            .assert(hasStateDescription("Unmuted"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
            .performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun muteReportsMutedWhenMuted() {
        controls(isMuted = true)
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assert(hasStateDescription("Muted"))
    }

    @Test
    fun speakerReportsOnAndOffAndTogglesOnClick() {
        var toggles = 0
        controls(isSpeakerOn = true, onToggleSpeaker = { toggles++ })
        rule
            .onNodeWithTag(CALL_CONTROL_SPEAKER_TAG)
            .assert(hasStateDescription("On"))
            .performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun speakerReportsOffWhenTheSpeakerIsOff() {
        controls(isSpeakerOn = false)
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assert(hasStateDescription("Off"))
    }

    @Test
    fun externalControlsHaveOnlyMinimizeAndChat() {
        controls(showAudioControls = false)
        rule.onNodeWithTag(CALL_CONTROL_MINIMIZE_TAG).assertExists()
        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).assertExists().assertIsNotEnabled()
        rule.onNodeWithTag(CALL_CONTROL_MUTE_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CALL_CONTROL_SPEAKER_TAG).assertDoesNotExist()
    }

    // ---- MeetingCallEndButton ----

    @Test
    fun endButtonShowsItsLabelAndClicksOnce() {
        var clicks = 0
        show { MeetingCallEndButton(label = "End Call", isEnding = false, onClick = { clicks++ }) }
        rule.onNodeWithText("End Call").assertExists()
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun endButtonShowsEndSessionForTheExternalVariant() {
        show { MeetingCallEndButton(label = "End Session", isEnding = false, onClick = {}) }
        rule.onNodeWithTag(CALL_END_BUTTON_TAG).assertExists()
        rule.onNodeWithText("End Session").assertExists()
    }

    @Test
    fun endButtonWhileEndingShowsASpinnerAnnouncesEndingAndIgnoresTaps() {
        var clicks = 0
        show { MeetingCallEndButton(label = "End Call", isEnding = true, onClick = { clicks++ }) }
        rule.onNodeWithText("End Call").assertDoesNotExist()
        rule
            .onNodeWithTag(CALL_END_BUTTON_TAG)
            .assert(hasStateDescription("Ending call"))
            .performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun endButtonDoesNotAnnounceEndingWhenIdle() {
        show { MeetingCallEndButton(label = "End Call", isEnding = false, onClick = {}) }
        rule
            .onNodeWithTag(CALL_END_BUTTON_TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        rule.onNodeWithText("End Call").assertTextEquals("End Call")
    }
}
