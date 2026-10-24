package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeetingCallPillTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var taps = 0

    private fun setPill(
        peerName: String? = "Ada",
        elapsed: Long = 65,
    ) {
        composeRule.setContent {
            ClickMeTheme {
                MeetingCallPill(
                    peerName = peerName,
                    peerAvatarUrl = null,
                    timer = CallTimerState(elapsedSeconds = elapsed, remainingSeconds = 1000),
                    onTap = { taps++ },
                )
            }
        }
    }

    @Test
    fun pillIsOneMergedButtonWithReturnToCallLabel() {
        setPill()
        val pill = composeRule.onNodeWithTag(MEETING_CALL_PILL_TAG)
        pill.assertTextContains("01:05")
        pill.assertContentDescriptionEquals("Return to call with Ada")
        pill.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        pill.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        val click = pill.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Return to call", click?.label)
    }

    @Test
    fun tappingTheWholePillCallsOnTapOnce() {
        setPill()
        composeRule.onNodeWithTag(MEETING_CALL_PILL_TAG).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun nullPeerNameFallsBackToPlainLabel() {
        setPill(peerName = null)
        composeRule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertContentDescriptionEquals("Return to call")
    }

    @Test
    fun beforeStartTheTimerReadsStartsIn() {
        setPill(elapsed = -90)
        composeRule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertTextContains("Starts in 1:30")
    }

    @Test
    fun touchTargetIsAtLeast48dp() {
        setPill()
        composeRule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertHeightIsAtLeast(48.dp)
    }
}
