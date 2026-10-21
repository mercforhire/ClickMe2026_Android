package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.AVATAR_LOCAL_IMAGE_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp")
class SignupProfilePhotoScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private class Calls {
        var upload = 0
        var adjust = 0
        var remove = 0
        var confirm = 0
        var dismissRemove = 0
        var save = 0
    }

    private fun show(
        state: SignupProfilePhotoUiState = SignupProfilePhotoUiState(),
        calls: Calls = Calls(),
    ) {
        rule.setContent {
            ClickMeTheme {
                SignupProfilePhotoContent(
                    state = state,
                    onUpload = { calls.upload++ },
                    onAdjustCrop = { calls.adjust++ },
                    onRemove = { calls.remove++ },
                    onConfirmRemove = { calls.confirm++ },
                    onDismissRemove = { calls.dismissRemove++ },
                    onSave = { calls.save++ },
                    onBack = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    @Test
    fun withNoPhoto_showsHeaderUploadAndSaveOnly() {
        show()
        rule.onAllNodesWithText("Profile Photo").assertCountEquals(1)
        rule.onNodeWithText("Add a Profile Photo").assertIsDisplayed()
        rule.onNodeWithText("Choose a clear photo so clients can recognize you.").assertIsDisplayed()
        rule.onNodeWithContentDescription("No profile photo yet").assertExists()
        rule.onNodeWithText("Upload Photo").assertIsDisplayed()
        rule.onNodeWithText("Save").assertIsDisplayed()
        rule.onAllNodesWithText("Adjust Crop").assertCountEquals(0)
        rule.onAllNodesWithText("Remove Photo").assertCountEquals(0)
    }

    @Test
    fun withASavedAvatar_offersChooseAnotherAndRemoveButNoAdjust() {
        show(SignupProfilePhotoUiState(avatarUrl = "https://cdn.example/a.jpg"))
        rule.onNodeWithContentDescription("Profile photo preview").assertExists()
        rule.onNodeWithText("Choose Another Photo").assertIsDisplayed()
        rule.onNodeWithText("Remove Photo").assertIsDisplayed()
        rule.onAllNodesWithText("Upload Photo").assertCountEquals(0)
        rule.onAllNodesWithText("Adjust Crop").assertCountEquals(0)
    }

    @Test
    fun withALocalPreview_showsTheImageAndAdjustAndRemoveWork() {
        val calls = Calls()
        show(SignupProfilePhotoUiState(localPreview = ImageBitmap(4, 4), hasSource = true), calls)
        rule.onNodeWithTag(AVATAR_LOCAL_IMAGE_TAG, useUnmergedTree = true).assertExists()

        rule.onNodeWithText("Adjust Crop").performClick()
        rule.onNodeWithText("Remove Photo").performClick()

        assertEquals(1, calls.adjust)
        assertEquals(1, calls.remove)
    }

    @Test
    fun removeConfirmation_showsCopyAndRoutesBothButtons() {
        val calls = Calls()
        show(SignupProfilePhotoUiState(avatarUrl = "https://cdn.example/a.jpg", showRemoveConfirm = true), calls)
        rule.onNodeWithText("Remove photo?").assertIsDisplayed()
        rule.onNodeWithText("Your profile photo will be deleted. You can upload a new one anytime.").assertIsDisplayed()

        rule.onNodeWithText("Remove").performClick()
        rule.onNodeWithText("Cancel").performClick()

        assertEquals(1, calls.confirm)
        assertEquals(1, calls.dismissRemove)
    }

    @Test
    fun uploadErrorDialog_usesTheUploadTitle() {
        show(
            SignupProfilePhotoUiState(
                dialogError = FieldError.Text("Server said no"),
                dialogTitleRes = R.string.signup_photo_upload_error_title,
            ),
        )
        rule.onNodeWithText("Couldn't upload photo").assertIsDisplayed()
        rule.onNodeWithText("Server said no").assertIsDisplayed()
        rule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun uploadAndSaveButtons_callTheirCallbacks() {
        val calls = Calls()
        show(calls = calls)

        rule.onNode(hasText("Upload Photo") and hasClickAction()).performClick()
        rule.onNode(hasText("Save") and hasClickAction()).performClick()

        assertEquals(1, calls.upload)
        assertEquals(1, calls.save)
    }
}
