package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.testing.topicItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun setCard(
        item: ExpertTopicItem = topicItem(),
        isDeleting: Boolean = false,
        menuEnabled: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                TopicCard(
                    item = item,
                    isDeleting = isDeleting,
                    menuEnabled = menuEnabled,
                    onClick = { clicks += "card" },
                    onEdit = { clicks += "edit" },
                    onDelete = { clicks += "delete" },
                )
            }
        }
    }

    private val price = Money.format(2500, "CAD")

    @Test
    fun paidCardShowsTitlePriceAndDuration() {
        setCard()

        rule.onNodeWithText("Tax planning").assertExists()
        rule.onNodeWithText(price).assertExists()
        rule.onNodeWithText("Per session · 1 hour").assertExists()
    }

    @Test
    fun freeCardShowsFreeNotTheStoredAmount() {
        setCard(topicItem(isFree = true, amount = 2500))

        rule.onNodeWithText("Free").assertExists()
        rule.onNodeWithText(price).assertDoesNotExist()
    }

    @Test
    fun missingDurationShowsPlainPerSession() {
        setCard(topicItem(durationMins = null))

        rule.onNodeWithText("Per session").assertExists()
    }

    @Test
    fun cardTapCallsOnClick() {
        setCard()

        rule.onNodeWithTag(topicCardTag("t1")).performClick()

        assertEquals(listOf("card"), clicks)
    }

    @Test
    fun overflowMenuOffersEditAndDelete() {
        setCard()

        rule.onNodeWithTag(topicOverflowTag("t1")).performClick()
        rule.onNodeWithText("Edit").assertExists()
        rule.onNodeWithText("Delete").assertExists()
        rule.onNodeWithText("Edit").performClick()

        assertEquals(listOf("edit"), clicks)
    }

    @Test
    fun overflowDeleteCallsOnDelete() {
        setCard()

        rule.onNodeWithTag(topicOverflowTag("t1")).performClick()
        rule.onNodeWithText("Delete").performClick()

        assertEquals(listOf("delete"), clicks)
    }

    @Test
    fun deletingHidesOverflowShowsSpinnerAndIgnoresCardTap() {
        setCard(isDeleting = true, menuEnabled = false)

        rule.onNodeWithTag(topicOverflowTag("t1")).assertDoesNotExist()
        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Deleting"))
            .assertExists()
        rule.onNodeWithTag(topicCardTag("t1")).performClick()

        assertEquals(emptyList<String>(), clicks)
    }

    @Test
    fun overflowIsDisabledWhenAnotherOperationRuns() {
        setCard(menuEnabled = false)

        rule.onNodeWithTag(topicOverflowTag("t1")).assertIsNotEnabled()
    }

    @Test
    fun overflowIsEnabledWhenIdle() {
        setCard()

        rule.onNodeWithTag(topicOverflowTag("t1")).assertIsEnabled()
    }

    @Test
    fun cardExposesMergedDescriptionAndClickLabel() {
        setCard()

        rule.onNodeWithContentDescription("Tax planning, $price, 1 hour").assertExists()
        rule
            .onNode(
                SemanticsMatcher("onClickLabel == Edit topic") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Edit topic"
                },
            ).assertExists()
        rule.onNodeWithContentDescription("More options for Tax planning").assertExists()
    }

    @Test
    fun deleteDialogShowsTitleMessageAndButtons() {
        rule.setContent { ClickMeTheme { TopicDeleteDialog("Tax planning", onConfirm = {}, onDismiss = {}) } }

        rule.onNodeWithText("Delete this topic?").assertExists()
        rule
            .onNodeWithText("\u201CTax planning\u201D will be removed and clients won't be able to book it.")
            .assertExists()
        rule.onNodeWithText("Keep Topic").assertExists()
        rule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun deleteDialogConfirmCallsOnConfirmOnly() {
        rule.setContent {
            ClickMeTheme {
                TopicDeleteDialog(
                    topicTitle = "Tax planning",
                    onConfirm = { clicks += "confirm" },
                    onDismiss = { clicks += "dismiss" },
                )
            }
        }

        rule.onNodeWithText("Delete").performClick()

        assertEquals(listOf("confirm"), clicks)
    }

    @Test
    fun deleteDialogKeepTopicDismissesWithoutConfirming() {
        rule.setContent {
            ClickMeTheme {
                TopicDeleteDialog(
                    topicTitle = "Tax planning",
                    onConfirm = { clicks += "confirm" },
                    onDismiss = { clicks += "dismiss" },
                )
            }
        }

        rule.onNodeWithText("Keep Topic").performClick()

        assertEquals(listOf("dismiss"), clicks)
    }

    @Test
    fun emptyContentShowsCopyAndAddTopicButton() {
        rule.setContent { ClickMeTheme { TopicsEmptyContent(onAddTopic = { clicks += "add" }) } }

        rule.onNodeWithText("Add your first topic").assertExists()
        rule.onNodeWithText("Clients can't book you until you add at least one topic.").assertExists()
        rule.onNodeWithText("Add Topic").performClick()

        assertEquals(listOf("add"), clicks)
    }

    @Test
    fun errorContentShowsMessageAndRetry() {
        rule.setContent { ClickMeTheme { TopicsErrorContent("Server down", onRetry = { clicks += "retry" }) } }

        rule.onNodeWithText("Couldn't load topics").assertExists()
        rule.onNodeWithText("Server down").assertExists()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(listOf("retry"), clicks)
    }

    @Test
    fun skeletonRendersThreeBlocks() {
        rule.setContent { ClickMeTheme { TopicsSkeleton() } }

        rule.onNodeWithTag(TOPICS_SKELETON_TAG).assertExists()
        rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).assertCountEquals(3)
    }
}
