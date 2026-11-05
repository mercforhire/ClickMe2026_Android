package com.uptrendinvestments.clickmeapp.screens.signup

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.uptrendinvestments.clickmeapp.screens.signup.overview.SetupStepRow
import com.uptrendinvestments.clickmeapp.screens.signup.overview.SignupOverviewContent
import com.uptrendinvestments.clickmeapp.screens.signup.overview.SignupOverviewUiState
import com.uptrendinvestments.clickmeapp.screens.signup.review.SignupReviewContent
import com.uptrendinvestments.clickmeapp.screens.signup.review.SignupReviewUiState
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.navigationBarInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BUG-30: the Profile Setup Next button sat behind the system navigation bar (Samsung 3-button navigation), because the
 * Scaffold `bottomBar` is not inset automatically. The Review screen's Publish bar had the same gap. With a 48 dp
 * navigation bar on a 800 dp tall window, the button must end above 752 dp. `w360dp-h800dp` is mdpi, so 1 px = 1 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SignupBottomBarInsetRegressionTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private fun buttonBottom(label: String): Float {
        rule.dispatchInsets(view, navigationBarInsets(bottomPx = NAV_BAR_PX))
        return rule
            .onNode(hasClickAction() and hasText(label))
            .getUnclippedBoundsInRoot()
            .bottom.value
    }

    @Test
    fun profileSetupNextClearsTheNavigationBar() {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                SignupOverviewContent(
                    state =
                        SignupOverviewUiState(
                            rows = SetupStep.entries.map { SetupStepRow(it, false) },
                            completedCount = 0,
                        ),
                    onOpenStep = {},
                    onNext = {},
                    onNotNow = {},
                    onRefresh = {},
                    onRefreshErrorShown = {},
                    onRequestLogout = {},
                    onDismissLogout = {},
                    onConfirmLogout = {},
                )
            }
        }
        val bottom = buttonBottom("Next")
        assertTrue(
            "Next ends at $bottom dp, inside the nav bar that starts at $NAV_BAR_TOP dp",
            bottom <= NAV_BAR_TOP,
        )
    }

    @Test
    fun profileReviewPublishClearsTheNavigationBar() {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                SignupReviewContent(
                    state =
                        SignupReviewUiState(
                            firstName = "Ada",
                            location = "Toronto, ON, Canada",
                            languages = "English",
                            timezoneLabel = "(GMT-05:00) Eastern Time",
                            avatarUrl = null,
                            skills = "Tax",
                            missing = emptyList(),
                            canPublish = true,
                        ),
                    onEdit = {},
                    onPublish = {},
                    onBack = {},
                    onDismissMissingFields = {},
                    onDismissDialog = {},
                )
            }
        }
        val bottom = buttonBottom("Publish Profile")
        assertTrue(
            "Publish ends at $bottom dp, inside the nav bar that starts at $NAV_BAR_TOP dp",
            bottom <= NAV_BAR_TOP,
        )
    }

    private companion object {
        const val NAV_BAR_PX = 48
        const val NAV_BAR_TOP = 800f - NAV_BAR_PX
    }
}
