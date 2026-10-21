package com.uptrendinvestments.clickmeapp.screens

import android.content.Intent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.login.SuspendedAccountDialog
import com.uptrendinvestments.clickmeapp.screens.login.supportEmailIntent
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RootScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private var dismissCount = 0
    private var supportCount = 0

    private fun showDialog() {
        rule.setContent {
            ClickMeTheme {
                SuspendedAccountDialog(
                    onDismiss = { dismissCount++ },
                    onContactSupport = { supportCount++ },
                )
            }
        }
    }

    @Test
    fun suspendedDialogShowsTitleAndSupportEmail() {
        showDialog()

        rule.onNodeWithText("Your account has been suspended").assertExists()
        rule.onNode(hasText("support@clickme.app", substring = true)).assertExists()
    }

    @Test
    fun suspendedDialogOkCallsOnDismiss() {
        showDialog()

        rule.onNodeWithText("OK").performClick()

        assertEquals(1, dismissCount)
        assertEquals(0, supportCount)
    }

    @Test
    fun suspendedDialogEmailSupportCallsOnContactSupport() {
        showDialog()

        rule.onNodeWithText("Email support").performClick()

        assertEquals(1, supportCount)
    }

    @Test
    fun supportEmailIntentIsSendToMailtoSupportAddress() {
        val intent = supportEmailIntent()

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:support@clickme.app", intent.dataString)
    }
}
