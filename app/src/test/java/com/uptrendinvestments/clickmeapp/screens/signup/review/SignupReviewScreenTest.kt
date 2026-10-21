package com.uptrendinvestments.clickmeapp.screens.signup.review

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    @Test
    fun missingItemsShowStillNeededLineAndDisablePublish() {
        var published = 0
        show(empty.copy(missing = listOf(SetupStep.ProfilePhoto, SetupStep.Expertise)), onPublish = { published++ })

        val line = rule.onNodeWithText("Still needed: profile picture, expertise.")
        line.assertIsDisplayed()
        val live = line.fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, live)
        rule.onNode(hasText("Publish Profile") and hasClickAction()).assertIsNotEnabled()
        assertEquals(0, published)
    }

    @Test
    fun canPublishHidesStillNeededAndPublishCallsBack() {
        var published = 0
        show(full, onPublish = { published++ })

        rule.onAllNodesWithText("Still needed", substring = true).assertCountEquals(0)
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
}
