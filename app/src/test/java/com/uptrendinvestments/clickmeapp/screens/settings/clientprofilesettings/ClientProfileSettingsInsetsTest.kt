package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * POL-03 for one Settings form: the last Profile Settings field and its error line stay above the keyboard, and the
 * form column is capped at 600 dp on a large window (CONTEXT D-02, D-16 tier 4).
 */
@RunWith(RobolectricTestRunner::class)
class ClientProfileSettingsInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private fun show() {
        val state =
            ClientProfileSettingsUiState(
                snapshot = ProfileSnapshot.from(userProfileData(city = "Toronto", stateProvince = "ON")),
                email = "ana@example.com",
                fieldErrors = mapOf(ProfileTextFieldId.StateProvince to FieldError.Text("Pick a real province")),
            )
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                ClientProfileSettingsContent(
                    state = state,
                    onBack = {},
                    onFieldChange = { _, _ -> },
                    onRetrySave = {},
                    onRetryLoad = {},
                    onDismissDialog = {},
                    onOpenProfessional = {},
                    onApplyProfessional = { _, _ -> },
                    onDiscardProfessional = {},
                    onChangePhoto = {},
                    onDismissAvatarError = {},
                    onOpenLanguages = {},
                    onLanguageQueryChange = {},
                    onToggleLanguage = {},
                    onRetryLanguages = {},
                    onDismissLanguages = {},
                    onApplyLanguages = {},
                    onOpenCountry = {},
                    onCountryQueryChange = {},
                    onSelectCountry = {},
                    onDismissCountry = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun focusedFieldAboveIme() {
        show()
        rule.dispatchInsets(view, imeInsets(bottomPx = 300))
        val field =
            rule.onNode(
                hasSetTextAction() and
                    hasAnyAncestor(hasTestTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.StateProvince.name)),
            )
        // The form scrolls inside the IME-shrunk viewport; scroll the field into it, then measure.
        field.performScrollTo()
        val fieldBottom = field.getUnclippedBoundsInRoot().bottom.value
        assertTrue("field bottom $fieldBottom dp must be at or above the IME top (500 dp)", fieldBottom <= 500f)
        val error = rule.onNodeWithText("Pick a real province")
        error.performScrollTo()
        val errorBottom = error.getUnclippedBoundsInRoot().bottom.value
        assertTrue("error bottom $errorBottom dp must be at or above the IME top (500 dp)", errorBottom <= 500f)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        show()
        val bounds = rule.onAllNodes(hasSetTextAction())[0].getUnclippedBoundsInRoot()
        val width = bounds.right.value - bounds.left.value
        assertTrue("field is $width dp wide, expected <= 600", width <= 600f)
    }
}
