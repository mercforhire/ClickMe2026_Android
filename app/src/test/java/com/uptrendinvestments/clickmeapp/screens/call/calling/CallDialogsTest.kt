package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.app.Application
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.GuardMessage
import com.uptrendinvestments.clickmeapp.managers.JoinErrorReason
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CallDialogsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        dialog: CallDialog?,
        onDismiss: () -> Unit = {},
    ) {
        rule.setContent { ClickMeTheme { CallDialogHost(dialog = dialog, onDismiss = onDismiss) } }
        rule.waitForIdle()
    }

    private fun assertDialog(
        dialog: CallDialog,
        title: String,
        message: String,
    ) {
        show(dialog)
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithText(message).assertIsDisplayed()
        rule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun nullDialog_rendersNothing() {
        show(null)
        rule.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun joinError_server_showsServerMessageAndOk() {
        var dismissed = 0
        show(CallDialog.JoinError(JoinErrorReason.Server("Session join window is not active.")), { dismissed++ })
        rule.onNodeWithText("Couldn't join session").assertIsDisplayed()
        rule.onNodeWithText("Session join window is not active.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun joinError_notSetUpForInAppVoice() {
        assertDialog(
            CallDialog.JoinError(JoinErrorReason.NotSetUpForInAppVoice),
            "Couldn't join session",
            "This booking isn't set up for in-app voice.",
        )
    }

    @Test
    fun joinError_missingMeetingLink() {
        assertDialog(
            CallDialog.JoinError(JoinErrorReason.MissingMeetingLink),
            "Couldn't join session",
            "This booking is missing a meeting link. Contact the other party.",
        )
    }

    @Test
    fun joinError_invalidMeetingLink() {
        assertDialog(
            CallDialog.JoinError(JoinErrorReason.InvalidMeetingLink),
            "Couldn't join session",
            "This booking's meeting link can't be opened. Contact the other party.",
        )
    }

    @Test
    fun externalLinkFailed() {
        assertDialog(
            CallDialog.ExternalLinkFailed,
            "Couldn't open meeting",
            "No app on this device could open the meeting link. Use Copy Link to open it another way.",
        )
    }

    @Test
    fun joinError_network() {
        assertDialog(
            CallDialog.JoinError(JoinErrorReason.Network),
            "Couldn't join session",
            "Check your connection and try again.",
        )
    }

    @Test
    fun joinError_unknown() {
        assertDialog(
            CallDialog.JoinError(JoinErrorReason.Unknown),
            "Couldn't join session",
            "Something went wrong. Please try again.",
        )
    }

    @Test
    fun guard_endCallToSwitchModes() {
        assertDialog(
            CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.SwitchModes)),
            "Can't do that during a call",
            "End the call to switch modes.",
        )
    }

    @Test
    fun guard_endCallToLogOut() {
        assertDialog(
            CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.LogOut)),
            "Can't do that during a call",
            "End the call to log out.",
        )
    }

    @Test
    fun guard_endCallToAddPaymentMethod() {
        assertDialog(
            CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.AddPaymentMethod)),
            "Can't do that during a call",
            "End the call to add a payment method.",
        )
    }

    @Test
    fun guard_sessionInProgressCancelBooking() {
        assertDialog(
            CallDialog.Guard(GuardMessage.SessionInProgress(RestrictedAction.CancelBooking)),
            "Can't do that during a call",
            "This session is in progress. End the call to cancel this booking.",
        )
    }

    @Test
    fun guard_alreadyOnCall() {
        assertDialog(
            CallDialog.Guard(GuardMessage.AlreadyOnCall),
            "Can't do that during a call",
            "You're already on a call. End it before starting another.",
        )
    }

    @Test
    fun timeLimit_showsCopy() {
        assertDialog(
            CallDialog.TimeLimit,
            "Call ended",
            "This session reached its time limit, so the call was ended.",
        )
    }

    @Test
    fun micDenied_showsOpenSettingsAndCancel() {
        show(CallDialog.MicPermissionDenied)
        rule.onNodeWithText("Microphone access needed").assertIsDisplayed()
        rule.onNodeWithText(DENIED_MESSAGE).assertIsDisplayed()
        rule.onNodeWithText("Open Settings").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun micDenied_openSettings_startsAppDetailsAndDismisses() {
        var dismissed = 0
        show(CallDialog.MicPermissionDenied, { dismissed++ })
        rule.onNodeWithTag(CALL_DIALOG_OPEN_SETTINGS_TAG).performClick()
        rule.waitForIdle()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val started = shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
        assertEquals("package:${app.packageName}", started.dataString)
        assertEquals(1, dismissed)
    }

    @Test
    fun micDenied_cancel_dismissesWithoutLaunching() {
        var dismissed = 0
        show(CallDialog.MicPermissionDenied, { dismissed++ })
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(1, dismissed)
        assertNull(shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity)
    }

    @Test
    fun rationale_continueAndNotNowCallTheirCallbacks() {
        var continued = 0
        var notNow = 0
        rule.setContent {
            ClickMeTheme { MicRationaleDialog(onContinue = { continued++ }, onNotNow = { notNow++ }) }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Microphone access needed").assertIsDisplayed()
        rule.onNodeWithText(RATIONALE_MESSAGE).assertIsDisplayed()
        rule.onNodeWithTag(CALL_DIALOG_RATIONALE_CONTINUE_TAG).performClick()
        rule.waitForIdle()
        assertEquals(1, continued)
        assertEquals(0, notNow)
        rule.onNodeWithTag(CALL_DIALOG_RATIONALE_NOT_NOW_TAG).performClick()
        rule.waitForIdle()
        assertEquals(1, notNow)
    }

    private companion object {
        const val DENIED_MESSAGE =
            "ClickMe needs microphone access to join voice sessions. " +
                "Tap Open Settings, then Permissions, then Microphone to enable it."
        const val RATIONALE_MESSAGE =
            "ClickMe needs your microphone so the other person can hear you during the session."
    }
}
