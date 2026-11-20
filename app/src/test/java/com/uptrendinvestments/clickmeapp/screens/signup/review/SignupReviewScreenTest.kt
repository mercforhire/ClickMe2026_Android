package com.uptrendinvestments.clickmeapp.screens.signup.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SignupReviewScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val full =
        SignupReviewUiState(
            firstName = "Ada",
            location = "Toronto, ON, Canada",
            languages = "English",
            timezoneLabel = "(GMT-05:00) Eastern Time",
            avatarUrl = "https://cdn.example/a.jpg",
            skills = "Tax",
            missing = emptyList(),
            canPublish = true,
        )

    private val empty =
        SignupReviewUiState(
            firstName = null,
            location = null,
            languages = null,
            timezoneLabel = "(GMT-05:00) Eastern Time",
            avatarUrl = null,
            skills = null,
            missing = listOf(SetupStep.PersonalDetails, SetupStep.ProfilePhoto, SetupStep.Expertise),
            canPublish = false,
        )

    private fun show(
        state: SignupReviewUiState,
        onEdit: (SetupStep) -> Unit = {},
        onPublish: () -> Unit = {},
        onDismissMissingFields: () -> Unit = {},
        onDismissDialog: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                SignupReviewContent(
                    state = state,
                    onEdit = onEdit,
                    onPublish = onPublish,
                    onBack = {},
                    onDismissMissingFields = onDismissMissingFields,
                    onDismissDialog = onDismissDialog,
                )
            }
        }
    }

    @Test
    fun rendersTitleCardsLabelsAndValues() {
        show(full)
        rule.onNodeWithText("Profile Review").assertIsDisplayed()
        rule.onNodeWithText("Personal Details").assertExists()
        rule.onNodeWithText("Profile Photo").assertExists()
        rule.onNodeWithText("Expertise").assertExists()
        rule.onNodeWithText("First Name").assertExists()
        rule.onNodeWithText("Ada").assertExists()
        rule.onNodeWithText("Location").assertExists()
        rule.onNodeWithText("Toronto, ON, Canada").assertExists()
        rule.onNodeWithText("Spoken Languages").assertExists()
        rule.onNodeWithText("English").assertExists()
        rule.onNodeWithText("(GMT-05:00) Eastern Time").assertExists()
        rule.onNodeWithText("Skills").assertExists()
        rule.onNodeWithText("Tax").assertExists()
        rule.onAllNodesWithText("Required").assertCountEquals(0)
    }

    @Test
    fun missingValuesShowRequired() {
        show(empty)
        // First name, location, languages, photo and skills.
        rule.onAllNodesWithText("Required").assertCountEquals(5)
    }

    @Test
    fun editButtonsOpenTheMatchingStep() {
        val opened = mutableListOf<SetupStep>()
        show(full, onEdit = { opened += it })
        rule.onNodeWithContentDescription("Edit Expertise").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Edit Personal Details").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Edit Timezone").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Edit Profile Photo").performScrollTo().performClick()
        assertEquals(
            listOf(SetupStep.Expertise, SetupStep.PersonalDetails, SetupStep.Timezone, SetupStep.ProfilePhoto),
            opened,
        )
    }

    /** iOS 05-04 (SIGN-01): Publish stays usable with a step missing; the view model owns the refusal. */
    @Test
    fun missingItemsKeepPublishUsableWithNoStillNeededLine() {
        var published = 0
        show(empty.copy(missing = listOf(SetupStep.ProfilePhoto, SetupStep.Expertise)), onPublish = { published++ })

        rule.onAllNodesWithText("Still needed", substring = true).assertCountEquals(0)
        rule.onNode(hasText("Publish Profile") and hasClickAction()).assertIsEnabled().performClick()
        assertEquals(1, published)
    }

    @Test
    fun publishRefusalDialogNamesTheStepsAndOkDismisses() {
        var dismissed = 0
        show(
            empty.copy(publishRefusal = listOf(SetupStep.ProfilePhoto)),
            onDismissDialog = { dismissed++ },
        )
        rule.onNodeWithText("Couldn't publish profile").assertIsDisplayed()
        rule.onNodeWithText("Finish these first: Add profile picture.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun canPublishAndPublishCallsBack() {
        var published = 0
        show(full, onPublish = { published++ })

        rule.onNode(hasText("Publish Profile") and hasClickAction()).assertIsEnabled().performClick()
        assertEquals(1, published)
    }

    @Test
    fun missingFieldsDialogNamesTheStepsAndOkDismisses() {
        var dismissed = 0
        show(
            full.copy(missingFieldsDialog = listOf(SetupStep.PersonalDetails, SetupStep.ProfilePhoto)),
            onDismissMissingFields = { dismissed++ },
        )
        rule.onNodeWithText("Couldn't publish profile").assertIsDisplayed()
        rule.onNodeWithText("Finish these steps first: Personal details, Add profile picture.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun otherErrorsShowADialogWithTheSameTitle() {
        var dismissed = 0
        show(full.copy(dialogError = FieldError.Text("Server exploded")), onDismissDialog = { dismissed++ })
        rule.onNodeWithText("Couldn't publish profile").assertIsDisplayed()
        rule.onNodeWithText("Server exploded").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    /** D-07: while publishing the button is busy (announced) and a tap neither fires nor re-enters. */
    @Test
    fun publishInFlightIgnoresRepeatTaps() {
        var published = 0
        show(full.copy(saveState = SaveState.Saving), onPublish = { published++ })

        val busy =
            rule.onNode(
                hasClickAction() and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving"),
            )
        busy.assertExists()
        busy.performClick()
        busy.performClick()

        assertEquals(0, published)
    }

    /** D-07: a failed publish shows the error dialog; closing it leaves every reviewed value in place. */
    @Test
    fun publishFailureKeepsReview() {
        var state by mutableStateOf(full.copy(dialogError = FieldError.Text("Server exploded")))
        rule.setContent {
            ClickMeTheme {
                SignupReviewContent(
                    state = state,
                    onEdit = {},
                    onPublish = {},
                    onBack = {},
                    onDismissMissingFields = {},
                    onDismissDialog = { state = state.copy(dialogError = null) },
                )
            }
        }

        rule.onNodeWithText("Couldn't publish profile").assertIsDisplayed()
        rule.onNodeWithText("Server exploded").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()

        rule.onNodeWithText("Server exploded").assertDoesNotExist()
        listOf("Ada", "Toronto, ON, Canada", "English", "(GMT-05:00) Eastern Time", "Tax").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        rule.onNode(hasText("Publish Profile") and hasClickAction()).assertIsEnabled()
    }

    /** D-02: on a large window the card column is capped at 600 dp and centered. */
    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun reviewCappedOnLargeWindow() {
        show(full)

        val edit = rule.onNodeWithContentDescription("Edit Personal Details").getUnclippedBoundsInRoot()
        val label = rule.onNodeWithText("First Name").getUnclippedBoundsInRoot()
        assertTrue("card edit button ends at ${edit.right}", edit.right <= 750.dp)
        assertTrue("card label starts at ${label.left}", label.left >= 150.dp)
    }
}
