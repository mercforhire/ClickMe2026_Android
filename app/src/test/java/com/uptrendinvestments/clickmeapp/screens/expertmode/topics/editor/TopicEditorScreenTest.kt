package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TOPIC_ADD_EXPERTISE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TOPIC_DESCRIPTION_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TopicEditorScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private val law = ExpertiseTagItem(id = "law", label = "Law")
    private val audit = ExpertiseTagItem(id = "audit", label = "Audit")

    private val helperPrefix = "Leave blank or enter 0 for a free session — otherwise min "
    private val belowFloorPrefix = "A paid session must be at least "

    private fun setContent(
        state: TopicEditorUiState,
        actions: TopicEditorActions = TopicEditorActions(),
        onBack: () -> Unit = {},
        expertiseSheet: @Composable (() -> Unit, (List<ExpertiseTagItem>) -> Unit) -> Unit = { _, _ -> },
    ) {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                TopicEditorContent(
                    state = state,
                    actions = actions,
                    onBack = onBack,
                    expertiseSheet = expertiseSheet,
                )
            }
        }
    }

    private fun titleInput() = hasSetTextAction() and hasAnyAncestor(hasTestTag(TOPIC_EDITOR_TITLE_TAG))

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun addState_showsAddTitle_andSaveCallsAction() {
        var saves = 0
        setContent(TopicEditorUiState(isEdit = false), TopicEditorActions(save = { saves++ }))
        rule.onNodeWithText("Add Topic").assertExists()
        rule.onNodeWithTag(TOPIC_EDITOR_SAVE_TAG).performClick()
        assertEquals(1, saves)
    }

    @Test
    fun editState_showsEditTitle() {
        setContent(TopicEditorUiState(isEdit = true))
        rule.onNodeWithText("Edit Topic").assertExists()
    }

    @Test
    fun saving_showsSpinnerState_hidesSave_andDisablesBack() {
        setContent(TopicEditorUiState(isEdit = false, isSaving = true))
        rule.onNode(hasStateDescription("Saving topic")).assertExists()
        rule.onNodeWithTag(TOPIC_EDITOR_SAVE_TAG).assertDoesNotExist()
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
    }

    @Test
    fun idle_backArrowIsEnabled_andCallsOnBack() {
        var backs = 0
        setContent(TopicEditorUiState(isEdit = false), onBack = { backs++ })
        rule.onNodeWithContentDescription("Back").assertIsEnabled().performClick()
        assertEquals(1, backs)
    }

    @Test
    fun floorKnown_showsHelperWithFormattedFloor() {
        setContent(TopicEditorUiState(isEdit = false, currency = "CAD", floorMinor = 500))
        val expected = helperPrefix + Money.format(500, "CAD")
        rule.onNodeWithText(expected).performScrollTo().assertExists()
    }

    @Test
    fun floorUnknown_showsNoHelper() {
        setContent(TopicEditorUiState(isEdit = false, currency = "CAD", floorMinor = null))
        assertTrue(rule.onAllNodesWithText(helperPrefix, substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun belowFloorError_replacesHelper() {
        setContent(
            TopicEditorUiState(
                isEdit = false,
                currency = "CAD",
                floorMinor = 500,
                priceText = "2",
                priceError = PriceFieldError.BelowFloor(500, "CAD"),
            ),
        )
        rule.onNodeWithText(belowFloorPrefix + Money.format(500, "CAD") + ".").performScrollTo().assertExists()
        assertTrue(rule.onAllNodesWithText(helperPrefix, substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun serverPriceError_showsServerMessage() {
        setContent(
            TopicEditorUiState(
                isEdit = false,
                priceError = PriceFieldError.Server("A paid session must be at least CA$5."),
            ),
        )
        rule.onNodeWithText("A paid session must be at least CA$5.").performScrollTo().assertExists()
    }

    @Test
    fun invalidPriceError_showsRequiredCopy() {
        setContent(TopicEditorUiState(isEdit = false, priceError = PriceFieldError.Invalid))
        rule.onNodeWithText("Please fill in all required fields with valid values.").performScrollTo().assertExists()
    }

    @Test
    fun titleError_showsRequiredCopyUnderTitle() {
        setContent(
            TopicEditorUiState(
                isEdit = false,
                titleError = FieldError.Res(R.string.topic_editor_required_error),
            ),
        )
        rule.onNodeWithText("Please fill in all required fields with valid values.").assertExists()
    }

    @Test
    fun dialog_showsTitleAndServerMessage_andDismisses() {
        setContent(
            TopicEditorUiState(isEdit = false, dialog = FieldError.Text("Boom")),
            TopicEditorActions(),
        )
        rule.onNodeWithText("Couldn't save topic").assertExists()
        rule.onNodeWithText("Boom").assertExists()
    }

    @Test
    fun addExpertise_opensSlot_andSavedTagsReachActions() {
        val received = mutableListOf<List<ExpertiseTagItem>>()
        setContent(
            state = TopicEditorUiState(isEdit = false, tags = TopicTagsLoad.Loaded(emptyList())),
            actions = TopicEditorActions(onExpertiseSaved = { received += it }),
            expertiseSheet = { _, onSaved ->
                Text("sheet-open")
                TextButton(onClick = { onSaved(listOf(law)) }) {
                    Text("stub-save")
                }
            },
        )
        assertTrue(rule.onAllNodesWithText("sheet-open").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag(TOPIC_ADD_EXPERTISE_TAG).performScrollTo().performClick()
        rule.onNodeWithText("sheet-open").assertExists()
        rule.onNodeWithText("stub-save").performClick()
        assertEquals(listOf(listOf(law)), received)
        // The slot is closed once tags were saved.
        assertTrue(rule.onAllNodesWithText("sheet-open").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun typingInTitle_callsOnTitleChange() {
        val typed = mutableListOf<String>()
        setContent(TopicEditorUiState(isEdit = false), TopicEditorActions(onTitleChange = { typed += it }))
        rule.onNode(titleInput()).performTextInput("Pricing")
        assertEquals(listOf("Pricing"), typed)
    }

    @Test
    fun tappingTagChip_callsToggleTag() {
        val toggled = mutableListOf<ExpertiseTagItem>()
        setContent(
            TopicEditorUiState(
                isEdit = false,
                tags = TopicTagsLoad.Loaded(listOf(law, audit)),
                profileTags = listOf(law, audit),
            ),
            TopicEditorActions(toggleTag = { toggled += it }),
        )
        rule.onNodeWithText("Audit").performScrollTo().performClick()
        assertEquals(listOf(audit), toggled)
    }

    @Test
    fun saving_disablesFields() {
        setContent(
            TopicEditorUiState(
                isEdit = false,
                isSaving = true,
                tags = TopicTagsLoad.Loaded(listOf(law)),
                profileTags = listOf(law),
            ),
        )
        // A disabled text field exposes no SetText action.
        rule.onNode(titleInput()).assertDoesNotExist()
        rule.onNodeWithTag(TOPIC_ADD_EXPERTISE_TAG).assertIsNotEnabled()
    }

    @Test
    fun focusTarget_isHandledOnce() {
        var handled = 0
        setContent(
            TopicEditorUiState(isEdit = false, focusTarget = EditorField.Title),
            TopicEditorActions(onFocusHandled = { handled++ }),
        )
        rule.waitForIdle()
        assertEquals(1, handled)
    }

    // region Phase 15: load cells, keyboard, large window
    //
    // The editor never fetches the topic: an edit is seeded from the list item (TopicEditorKey, D-07), so there is no
    // form-level skeleton or prefill error. Only the Expertise section loads, and it has its own row and Retry.

    @Test
    fun editFormShowsSeededFieldsAtOnceWithoutAFormLoadingState() {
        setContent(TopicEditorUiState(isEdit = true, title = "Pricing deep dive", description = "Notes"))

        rule.onNodeWithText("Edit Topic").assertExists()
        rule.onNodeWithContentDescription("Loading").assertDoesNotExist()
        rule.onNodeWithText("Pricing deep dive").assertExists()
        rule.onNodeWithTag(TOPIC_EDITOR_SAVE_TAG).assertIsEnabled()
    }

    @Test
    fun blankCreateFormHasNoLoadingState() {
        setContent(TopicEditorUiState(isEdit = false, tags = TopicTagsLoad.Loaded(emptyList())))

        rule.onNodeWithText("Add Topic").assertExists()
        rule.onNodeWithContentDescription("Loading").assertDoesNotExist()
        rule.onNode(titleInput()).assertExists()
    }

    @Test
    fun tagsLoading_showsItsRowAndLeavesTheFormUsable() {
        setContent(TopicEditorUiState(isEdit = true, tags = TopicTagsLoad.Loading))

        rule.onNodeWithText("Loading tags…").performScrollTo().assertExists()
        rule.onNode(titleInput()).assertExists()
        rule.onNodeWithTag(TOPIC_EDITOR_SAVE_TAG).assertIsEnabled()
    }

    @Test
    fun tagsFailed_keepsTheAppBarAndFormAndRetryReRunsTheLoad() {
        var retries = 0
        setContent(
            TopicEditorUiState(isEdit = true, tags = TopicTagsLoad.Failed),
            TopicEditorActions(retryTags = { retries++ }),
        )

        rule.onNodeWithText("Edit Topic").assertExists()
        rule.onNode(titleInput()).assertExists()
        rule.onNodeWithText("Couldn't load your tags.").performScrollTo().assertExists()
        rule.onNodeWithText("Retry").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun focusedFieldAboveIme() {
        setContent(TopicEditorUiState(isEdit = false, tags = TopicTagsLoad.Loaded(emptyList())))
        rule.onNodeWithTag(TOPIC_DESCRIPTION_TAG).performScrollTo().performClick()

        rule.dispatchInsets(view, imeInsets(bottomPx = 300))

        // The editor is a zero-inset Scaffold that pads the IME itself: the column shrinks and scrolls.
        val field = rule.onNodeWithTag(TOPIC_DESCRIPTION_TAG)
        field.performScrollTo()
        val bottom = field.getUnclippedBoundsInRoot().bottom.value
        assertTrue("Description bottom $bottom dp must be at or above the IME top (500 dp)", bottom <= 500f)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        setContent(TopicEditorUiState(isEdit = false, tags = TopicTagsLoad.Loaded(emptyList())))

        val bounds = rule.onNode(titleInput()).getUnclippedBoundsInRoot()
        val width = bounds.right.value - bounds.left.value
        assertTrue("Title field is $width dp wide, expected <= 600", width <= 600f)
    }

    // endregion
}
