package com.uptrendinvestments.clickmeapp.components.safety

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ProfileSafetyViewModel
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ReportUserSheetTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

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

    /** The saved values the system keeps, handed to the next ViewModel as a copy (Activity recreation). */
    private fun SavedStateHandle.copyForRecreation() = SavedStateHandle(keys().associateWith { get<Any>(it) })

    private var handle = SavedStateHandle()

    private fun showWithViewModel(tester: StateRestorationTester) {
        tester.setContent {
            ClickMeTheme {
                val vm =
                    remember { ProfileSafetyViewModel(handle, ChatsRepository(FakeClickMeApi()), ChatSessionState()) }
                val state by vm.state.collectAsState()
                ReportUserSheetContent(
                    name = "Ada",
                    reason = state.reason,
                    details = state.details,
                    submitting = state.reportSubmitting,
                    onSelectReason = vm::selectReason,
                    onDetailsChange = vm::onDetailsChange,
                    onSubmit = {},
                    onCancel = {},
                )
            }
        }
    }

    @Test
    fun detailsSurviveRestore() {
        val tester = StateRestorationTester(rule)
        showWithViewModel(tester)
        rule.onNodeWithText("Spam").performClick()
        rule.onNodeWithTag(REPORT_DETAILS_FIELD_TAG).performScrollTo().performTextInput("spam links")
        rule.waitForIdle()

        handle = handle.copyForRecreation()
        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithTag(REPORT_REASON_ROW_TAG_PREFIX + UserReportReason.Spam.name).assertIsSelected()
        rule.onNodeWithTag(REPORT_DETAILS_FIELD_TAG).performScrollTo().assertTextContains("spam links")
        rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun restoredDetailsCapped() {
        handle =
            SavedStateHandle(
                mapOf("reason" to UserReportReason.Spam.wire, "details" to "x".repeat(REPORT_DETAILS_MAX + 50)),
            )
        val tester = StateRestorationTester(rule)
        showWithViewModel(tester)

        rule
            .onNodeWithTag(REPORT_DETAILS_FIELD_TAG)
            .performScrollTo()
            .assertTextEquals("x".repeat(REPORT_DETAILS_MAX), includeEditableText = true)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun submitAboveIme() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                ReportUserSheetContent(
                    name = "Ada",
                    reason = UserReportReason.Spam,
                    details = "",
                    submitting = false,
                    onSelectReason = {},
                    onDetailsChange = {},
                    onSubmit = {},
                    onCancel = {},
                )
            }
        }

        rule.dispatchInsets(view, imeInsets(bottomPx = 300))

        // The sheet body scrolls inside the space above the keyboard; scrolled to, both stay above it.
        val field = rule.onNodeWithTag(REPORT_DETAILS_FIELD_TAG).performScrollTo().getUnclippedBoundsInRoot()
        assertTrue("Details bottom ${field.bottom.value} must clear the IME", field.bottom.value <= IME_TOP)
        val submit = rule.onNodeWithTag(REPORT_SHEET_SUBMIT_TAG).performScrollTo().getUnclippedBoundsInRoot()
        assertTrue("Submit bottom ${submit.bottom.value} must clear the IME", submit.bottom.value <= IME_TOP)
    }

    private companion object {
        /** 800 dp window less the 300 px IME, with half a pixel of tolerance. */
        const val IME_TOP = 500.5f
    }
}
