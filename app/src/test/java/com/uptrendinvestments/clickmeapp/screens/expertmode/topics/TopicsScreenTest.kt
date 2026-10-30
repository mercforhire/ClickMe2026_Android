package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TOPICS_SKELETON_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.topicCardTag
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.topicOverflowTag
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.topicItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TopicsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val t1 = topicItem("t1", title = "Tax planning")
    private val t2 = topicItem("t2", title = "Fundraising")
    private val calls = mutableListOf<String>()

    private fun loaded(vararg topics: ExpertTopicItem) = TopicsUiState(list = TopicsListState.Loaded(topics.toList()))

    private fun setContent(state: TopicsUiState) {
        rule.setContent {
            ClickMeTheme {
                TopicsContent(
                    state = state,
                    onAddTopic = { calls += "add" },
                    onEditTopic = { calls += "edit:${it.id}" },
                    onOpenAvailability = { calls += "availability" },
                    onRetry = { calls += "retry" },
                    onRefresh = { calls += "refresh" },
                    onDelete = { calls += "delete:$it" },
                    onDismissDialog = { calls += "dismissDialog" },
                    onSnackbarShown = { calls += "snackbarShown" },
                )
            }
        }
    }

    @Test
    fun loadingShowsSkeletonAndNoFab() {
        setContent(TopicsUiState())

        rule.onNodeWithTag(TOPICS_SKELETON_TAG).assertExists()
        rule.onNodeWithTag(TOPICS_ADD_FAB_TAG).assertDoesNotExist()
    }

    @Test
    fun failedShowsMessageAndRetryCallsOnRetry() {
        setContent(TopicsUiState(list = TopicsListState.Failed(FieldError.Text("Server down"))))

        rule.onNodeWithText("Couldn't load topics").assertExists()
        rule.onNodeWithText("Server down").assertExists()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun emptyShowsAddFirstTopicWithoutFab() {
        setContent(loaded())

        rule.onNodeWithText("Add your first topic").assertExists()
        rule.onNodeWithTag(TOPICS_ADD_FAB_TAG).assertDoesNotExist()
        rule.onNodeWithText("Add Topic").performClick()

        assertEquals(listOf("add"), calls)
    }

    @Test
    fun loadedShowsCardsAndFabAddsTopic() {
        setContent(loaded(t1, t2))

        rule.onNodeWithText("Tax planning").assertExists()
        rule.onNodeWithText("Fundraising").assertExists()
        rule.onNodeWithContentDescription("Add topic").assertExists()
        rule.onNodeWithTag(TOPICS_ADD_FAB_TAG).performClick()

        assertEquals(listOf("add"), calls)
    }

    @Test
    fun cardTapAndOverflowEditOpenTheEditor() {
        setContent(loaded(t1, t2))

        rule.onNodeWithTag(topicCardTag("t1")).performClick()
        rule.onNodeWithTag(topicOverflowTag("t1")).performClick()
        rule.onNodeWithText("Edit").performClick()

        assertEquals(listOf("edit:t1", "edit:t1"), calls)
    }

    @Test
    fun deleteAsksFirstAndKeepTopicCancels() {
        setContent(loaded(t1, t2))

        rule.onNodeWithTag(topicOverflowTag("t1")).performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Delete this topic?").assertExists()
        rule.onNodeWithText("Keep Topic").performClick()

        rule.onNodeWithText("Delete this topic?").assertDoesNotExist()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun confirmingDeleteCallsOnDeleteAndClosesTheDialog() {
        setContent(loaded(t1, t2))

        rule.onNodeWithTag(topicOverflowTag("t1")).performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()

        rule.onNodeWithText("Delete this topic?").assertDoesNotExist()
        assertEquals(listOf("delete:t1"), calls)
    }

    @Test
    fun deletingShowsSpinnerOnTargetAndDisablesOtherOverflowButtons() {
        setContent(loaded(t1, t2).copy(deletingId = "t1"))

        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Deleting"))
            .assertExists()
        rule.onNodeWithTag(topicOverflowTag("t2")).assertIsNotEnabled()
    }

    @Test
    fun availabilityActionAndTitleAreInTheAppBar() {
        setContent(loaded(t1))

        rule.onNodeWithText("Topics").assertExists()
        rule.onNodeWithContentDescription("Availability").assertExists()
        rule.onNodeWithTag(TOPICS_AVAILABILITY_ACTION_TAG).performClick()

        assertEquals(listOf("availability"), calls)
    }

    @Test
    fun deletedSnackbarShowsMessageThenIsConsumed() {
        setContent(loaded(t2).copy(snackbar = TopicsSnackbar.Deleted))

        rule.waitForIdle()
        rule.onNodeWithText("Topic deleted").assertExists()
        assertEquals(listOf("snackbarShown"), calls)
    }

    @Test
    fun deleteErrorDialogShowsTitleAndMessageAndOkDismisses() {
        setContent(
            loaded(t1).copy(
                dialog = TopicsDialog(R.string.topics_delete_error_title, FieldError.Text("Topic not found")),
            ),
        )

        rule.onNodeWithText("Couldn't delete topic").assertExists()
        rule.onNodeWithText("Topic not found").assertExists()
        rule.onNodeWithText("OK").performClick()

        assertEquals(listOf("dismissDialog"), calls)
    }

    @Test
    fun refreshFailedShowsRetry() {
        setContent(loaded(t1).copy(snackbar = TopicsSnackbar.RefreshFailed))

        rule.waitForIdle()
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(listOf("snackbarShown"), calls)
        // Long duration: still up after the Short 4 s would have dismissed it.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(5_000)
        rule.onNodeWithText("Retry").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(listOf("snackbarShown", "refresh"), calls)
        rule.onNodeWithText("Tax planning").assertExists()
    }

    private fun assertResultSnackbarHasNoAction(
        kind: TopicsSnackbar,
        message: String,
    ) {
        setContent(loaded(t1).copy(snackbar = kind))

        rule.waitForIdle()
        rule.onNodeWithText(message).assertExists()
        rule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun addedSnackbarKeepsNoAction() = assertResultSnackbarHasNoAction(TopicsSnackbar.Added, "Topic added")

    @Test
    fun updatedSnackbarKeepsNoAction() = assertResultSnackbarHasNoAction(TopicsSnackbar.Updated, "Topic updated")

    @Test
    fun deletedSnackbarKeepsNoAction() = assertResultSnackbarHasNoAction(TopicsSnackbar.Deleted, "Topic deleted")

    @Test
    fun listCells() {
        var state by mutableStateOf(TopicsUiState())
        rule.setContent {
            ClickMeTheme {
                TopicsContent(
                    state = state,
                    onAddTopic = {},
                    onEditTopic = {},
                    onOpenAvailability = {},
                    onRetry = { calls += "retry" },
                    onRefresh = {},
                    onDelete = {},
                    onDismissDialog = {},
                    onSnackbarShown = {},
                )
            }
        }
        rule.onNodeWithTag(TOPICS_SKELETON_TAG).assertExists()

        state = state.copy(list = TopicsListState.Failed(FieldError.Text("Server down")))
        rule.waitForIdle()
        rule.onNodeWithText("Couldn't load topics").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), calls)

        state = state.copy(list = TopicsListState.Loaded(emptyList()))
        rule.waitForIdle()
        rule.onNodeWithText("Add your first topic").assertExists()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        setContent(loaded(t1))

        val bounds = rule.onNodeWithTag(topicCardTag("t1")).getUnclippedBoundsInRoot()
        assertTrue("width ${bounds.right - bounds.left}", (bounds.right - bounds.left).value <= 600f)
    }
}
