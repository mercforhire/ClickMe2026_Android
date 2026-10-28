package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthErrorDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultLabelIsOk() {
        var dismissed = 0
        rule.setContent {
            ClickMeTheme { AuthErrorDialog(message = "Server down", onDismiss = { dismissed++ }) }
        }
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun customLabelIsUsed() {
        var dismissed = 0
        rule.setContent {
            ClickMeTheme {
                AuthErrorDialog(message = "Server down", onDismiss = { dismissed++ }, confirmLabel = "Edit review")
            }
        }
        rule.onNodeWithText("OK").assertDoesNotExist()
        rule.onNodeWithText("Edit review").performClick()
        assertEquals(1, dismissed)
    }
}
