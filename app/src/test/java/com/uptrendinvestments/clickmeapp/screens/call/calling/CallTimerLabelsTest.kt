package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.Modifier
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallTimerLabelsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun assertTimerText(
        state: CallTimerState,
        expected: String,
    ) {
        show { Text(callTimerText(state), Modifier.testTag("t")) }
        rule.onNodeWithTag("t").assertTextEquals(expected)
    }

    private fun assertSpoken(
        state: CallTimerState,
        expected: String,
    ) {
        show { Text(callTimerSpokenDescription(state), Modifier.testTag("t")) }
        rule.onNodeWithTag("t").assertTextEquals(expected)
    }

    @Test
    fun beforeStartShowsStartsInCountdown() = assertTimerText(CallTimerState(-90, null), "Starts in 1:30")

    @Test
    fun atStartShowsZeroes() = assertTimerText(CallTimerState(0, null), "00:00")

    @Test
    fun afterStartShowsPaddedMinutesAndSeconds() = assertTimerText(CallTimerState(125, null), "02:05")

    @Test
    fun minutesPastFiftyNineKeepGrowing() = assertTimerText(CallTimerState(4512, null), "75:12")

    @Test
    fun chipShowsRemainingInFinalStretch() {
        show { Text(callRemainingChipText(CallTimerState(1500, 300)).orEmpty(), Modifier.testTag("t")) }
        rule.onNodeWithTag("t").assertTextEquals("5:00 left")
    }

    @Test
    fun chipIsNullOutsideFinalStretch() {
        var chip: String? = "unset"
        show { chip = callRemainingChipText(CallTimerState(1000, 800)) }
        rule.runOnIdle { assert(chip == null) }
    }

    @Test
    fun spokenDescriptionForElapsedUsesPlurals() = assertSpoken(CallTimerState(125, null), "2 minutes 5 seconds elapsed")

    @Test
    fun spokenDescriptionBeforeStartUsesSingulars() = assertSpoken(CallTimerState(-61, null), "Starts in 1 minute 1 second")

    @Test
    fun spokenDescriptionBeforeStartPluralMinutesAndSeconds() =
        assertSpoken(CallTimerState(-90, null), "Starts in 1 minute 30 seconds")
}
