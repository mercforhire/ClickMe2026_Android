package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.IntSize
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCropScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var zoomInCount = 0
    private var moves = mutableListOf<Pair<Int, Int>>()
    private var useCount = 0
    private var cancelCount = 0
    private var dismissCount = 0

    private val readyState =
        PhotoCropUiState(
            isLoading = false,
            source = ImageBitmap(200, 100),
            sourceSize = IntSize(200, 100),
            transform = CropTransform(2f, Offset.Zero),
            guideRadius = 50f,
        )

    private val buttonMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun show(state: PhotoCropUiState) {
        rule.setContent {
            ClickMeTheme {
                PhotoCropContent(
                    state = state,
                    onGuideMeasured = {},
                    onGesture = { _, _, _ -> },
                    onGestureEnd = {},
                    onDoubleTap = {},
                    onZoomIn = { zoomInCount++ },
                    onZoomOut = {},
                    onMove = { dx, dy -> moves += dx to dy },
                    onUse = { useCount++ },
                    onCancel = { cancelCount++ },
                    onDismissDialog = { dismissCount++ },
                )
            }
        }
    }

    private fun customActionLabels(): List<String> {
        val node = rule.onNodeWithContentDescription("Photo crop area").fetchSemanticsNode()
        return node.config[SemanticsActions.CustomActions].map { it.label }
    }

    private fun invokeCustomAction(label: String) {
        val node = rule.onNodeWithContentDescription("Photo crop area").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnUiThread { action.action() }
    }

    @Test
    fun loadingShowsProgressAndUsePhotoIsInert() {
        show(PhotoCropUiState())
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
        rule.onNodeWithText("Use Photo").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun cropAreaExposesSixCustomActions() {
        show(readyState)
        assertEquals(
            listOf("Zoom in", "Zoom out", "Move left", "Move right", "Move up", "Move down"),
            customActionLabels(),
        )
    }

    @Test
    fun customActionsInvokeCallbacks() {
        show(readyState)
        invokeCustomAction("Zoom in")
        invokeCustomAction("Move left")
        assertEquals(1, zoomInCount)
        assertEquals(listOf(-1 to 0), moves)
    }

    @Test
    fun hintIsShownAndButtonsInvokeCallbacks() {
        show(readyState)
        rule.onNodeWithText("Pinch to zoom and drag to reposition.").performScrollTo().assertExists()
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithText("Use Photo").performScrollTo().performClick()
        assertEquals(1, cancelCount)
        assertEquals(1, useCount)
    }

    @Test
    fun croppingDisablesBothButtons() {
        show(readyState.copy(isCropping = true))
        rule.onAllNodes(buttonMatcher).assertCountEquals(2)
        rule.onAllNodes(buttonMatcher)[0].assertIsNotEnabled()
        rule.onAllNodes(buttonMatcher)[1].assertIsNotEnabled()
    }

    @Test
    fun loadFailureDialogOkCancels() {
        show(
            PhotoCropUiState(
                isLoading = false,
                loadFailed = true,
                dialogError = FieldError.Res(R.string.signup_photo_open_error_body),
            ),
        )
        rule.onNodeWithText("Couldn't open photo").assertExists()
        rule.onNodeWithText("Pick a different photo and try again.").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, cancelCount)
        assertEquals(0, dismissCount)
    }

    @Test
    fun cropErrorDialogOkDismissesWithoutCancelling() {
        show(readyState.copy(dialogError = FieldError.Res(R.string.login_error_unknown)))
        rule.onNodeWithText("Something went wrong").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissCount)
        assertEquals(0, cancelCount)
    }
}
