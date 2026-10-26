package com.uptrendinvestments.clickmeapp.screens.expertmode

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.components.EXPERT_RESCHEDULE_BADGE_TAG
import com.uptrendinvestments.clickmeapp.components.ExpertRescheduleBadge
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpertSharedUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun meetingTypeLabels() {
        val wires = listOf("in_app_voice", "skype_zoom", null, "carrier_pigeon")
        rule.setContent {
            ClickMeTheme {
                wires.forEachIndexed { i, wire ->
                    Text(text = meetingTypeLabel(wire), modifier = Modifier.testTag("wire$i"))
                }
            }
        }
        rule.onNodeWithTag("wire0").assertTextEquals("In-App Voice")
        rule.onNodeWithTag("wire1").assertTextEquals("Skype/Zoom")
        rule.onNodeWithTag("wire2").assertTextEquals("In-App Voice")
        rule.onNodeWithTag("wire3").assertTextEquals("In-App Voice")
    }

    @Test
    fun clientNameFallback() {
        val names = listOf("  Ada ", null, "  ")
        rule.setContent {
            ClickMeTheme {
                names.forEachIndexed { i, name ->
                    Text(text = clientDisplayName(name), modifier = Modifier.testTag("name$i"))
                }
            }
        }
        rule.onNodeWithTag("name0").assertTextEquals("Ada")
        rule.onNodeWithTag("name1").assertTextEquals("Client")
        rule.onNodeWithTag("name2").assertTextEquals("Client")
    }

    @Test
    fun rescheduleBadgeTexts() {
        rule.setContent { ClickMeTheme { ExpertRescheduleBadge(initiatedByClient = true) } }
        rule.onNodeWithTag(EXPERT_RESCHEDULE_BADGE_TAG).assertExists()
        rule.onNodeWithText("Client proposed a new time · Tap to review").assertExists()
    }

    @Test
    fun rescheduleBadgeExpertInitiated() {
        rule.setContent { ClickMeTheme { ExpertRescheduleBadge(initiatedByClient = false) } }
        rule.onNodeWithTag(EXPERT_RESCHEDULE_BADGE_TAG).assertExists()
        rule.onNodeWithText("Reschedule pending client's response").assertExists()
    }
}
