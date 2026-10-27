package com.uptrendinvestments.clickmeapp.components.safety

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportUserSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private var submitted = 0
    private var cancelled = 0

    private fun show(
        reason: UserReportReason? = null,
        submitting: Boolean = false,
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ClickMeTheme {
                    var selected by remember { mutableStateOf(reason) }
                    ReportUserSheetContent(
                        name = "Ada",
                        reason = selected,
                        details = "",
                        submitting = submitting,
                        onSelectReason = { selected = it },
                        onDetailsChange = {},
                        onSubmit = { submitted++ },
                        onCancel = { cancelled++ },
                    )
                }
            }
        }
    }

    @Test
    fun submitDisabledUntilReason() {
        show()

        rule.onNodeWithText("Report Ada").assertExists()
        rule.onNodeWithText("Why are you reporting Ada?").assertExists()
        rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).assertIsNotEnabled()

        rule.onNodeWithText("Impersonation").performClick()
        rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).assertIsEnabled()
    }

    @Test
    fun submitAndCancelCallbacks() {
        show(UserReportReason.Spam)

        rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).performScrollTo().performClick()

        assertEquals(1, cancelled)
        assertEquals(1, submitted)
    }

    @Test
    fun inFlightShowsSpinnerAndDisablesCancel() {
        show(UserReportReason.Spam, submitting = true)

        rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).performScrollTo().assertIsNotEnabled()
        rule
            .onNodeWithTag(REPORT_SHEET_SUBMIT_TAG)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Submitting"),
            )
        rule.onAllNodesWithText("Submit report").assertCountEquals(0)
    }

    @Test
    fun largeFontStacksButtons() {
        show(UserReportReason.Spam, fontScale = 2f)

        rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).performScrollTo()
        val submitTop = rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).getUnclippedBoundsInRoot().top
        val cancelTop = rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).getUnclippedBoundsInRoot().top
        assertTrue(submitTop < cancelTop)
    }

    @Test
    fun normalFontKeepsButtonsSideBySide() {
        show(UserReportReason.Spam)

        rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).performScrollTo()
        val submitTop = rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).getUnclippedBoundsInRoot().top
        val cancelTop = rule.onNodeWithTag(REPORT_SHEET_CANCEL_TAG).getUnclippedBoundsInRoot().top
        assertEquals(submitTop, cancelTop)
    }
}
