package com.uptrendinvestments.clickmeapp.components.safety

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
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

    private fun show(reason: UserReportReason? = null) {
        rule.setContent {
            ClickMeTheme {
                var selected by remember { mutableStateOf(reason) }
                ReportUserSheetContent(
                    name = "Ada",
                    reason = selected,
                    details = "",
                    submitting = false,
                    onSelectReason = { selected = it },
                    onDetailsChange = {},
                    onSubmit = { submitted++ },
                    onCancel = { cancelled++ },
                )
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
}
