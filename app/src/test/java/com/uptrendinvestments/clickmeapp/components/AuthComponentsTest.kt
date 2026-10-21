package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun validatedField_trailingBadge_showsBadgeAndKeepsToggle() {
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "x",
                    onValueChange = {},
                    placeholder = "Confirm",
                    leadingIcon = Icons.Outlined.Lock,
                    error = null,
                    isSecure = true,
                    trailingBadge = { Icon(Icons.Filled.CheckCircle, contentDescription = "Passwords match") },
                )
            }
        }
        rule.onNodeWithContentDescription("Passwords match").assertIsDisplayed()
        rule.onNodeWithContentDescription("Show password").assertIsDisplayed()
    }

    @Test
    fun validatedField_noBadgeNotSecure_rendersNoTrailingNode() {
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "x",
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Lock,
                    error = null,
                    trailingBadge = null,
                )
            }
        }
        rule.onAllNodesWithContentDescription("Show password").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Passwords match").assertCountEquals(0)
    }

    @Test
    fun validatedField_contentType_composesAndForwardsInput() {
        var latest = ""
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "",
                    onValueChange = { latest = it },
                    placeholder = "Code",
                    leadingIcon = Icons.Outlined.Lock,
                    error = null,
                    contentType = ContentType.SmsOtpCode,
                )
            }
        }
        rule.onNode(hasSetTextAction()).performTextInput("123")
        assertEquals("123", latest)
    }

    @Test
    fun authErrorDialog_showsTitleMessageAndOk_andOkDismissesOnce() {
        var dismissed = 0
        rule.setContent {
            ClickMeTheme { AuthErrorDialog(message = "Server down", onDismiss = { dismissed++ }) }
        }
        rule.onNodeWithText("Something went wrong").assertIsDisplayed()
        rule.onNodeWithText("Server down").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun authTopAppBar_showsTitleAndBackInvokesCallback() {
        var backs = 0
        rule.setContent {
            ClickMeTheme { AuthTopAppBar(title = "Reset Password", onBack = { backs++ }) }
        }
        rule.onNodeWithText("Reset Password").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun authErrorDialog_customTitle_replacesDefault() {
        rule.setContent {
            ClickMeTheme { AuthErrorDialog(message = "x", onDismiss = {}, title = "Couldn't save") }
        }
        rule.onNodeWithText("Couldn't save").assertIsDisplayed()
        rule.onAllNodesWithText("Something went wrong").assertCountEquals(0)
    }

    @Test
    fun authTopAppBar_nullOnBack_hidesBackArrow() {
        rule.setContent {
            ClickMeTheme { AuthTopAppBar(title = "T", onBack = null) }
        }
        rule.onNodeWithText("T").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun authTopAppBar_actionsSlot_isShownAndClickable() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                AuthTopAppBar(
                    title = "T",
                    onBack = null,
                    containerColor = Color.Transparent,
                    actions = {
                        IconButton(onClick = { clicks++ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                    },
                )
            }
        }
        rule.onNodeWithContentDescription("More options").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun authTopAppBar_backDisabled_disablesArrow() {
        rule.setContent {
            ClickMeTheme { AuthTopAppBar(title = "T", onBack = {}, backEnabled = false) }
        }
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
    }

    @Test
    fun passwordStrengthMeter_exposesSingleContentDescription() {
        rule.setContent {
            ClickMeTheme { PasswordStrengthMeter(PasswordStrength.Fair) }
        }
        rule.onNodeWithContentDescription("Password strength: Fair").assertIsDisplayed()
    }
}
