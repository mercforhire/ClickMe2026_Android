package com.uptrendinvestments.clickmeapp.screens.settings.help

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.testing.faqArticle
import com.uptrendinvestments.clickmeapp.testing.faqCategory
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HelpScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val categories =
        listOf(
            faqCategory(
                id = "c1",
                title = "Account",
                order = 1,
                articles =
                    listOf(
                        faqArticle(id = "a1", question = "How do I reset my password?", snippet = "Use Settings."),
                    ),
            ),
            faqCategory(
                id = "c2",
                title = "Calls",
                order = 2,
                articles = listOf(faqArticle(id = "a2", question = "Why did my call drop?", snippet = null)),
            ),
            faqCategory(
                id = "c3",
                title = "Payments",
                order = 3,
                articles = listOf(faqArticle(id = "a3", question = "How do I get paid?", snippet = "Weekly.")),
            ),
        )

    private fun setContent(loaded: List<FaqCategory> = categories) {
        rule.setContent {
            ClickMeTheme {
                HelpContent(
                    state = HelpUiState(load = HelpLoad.Loaded(loaded)),
                    onBack = {},
                    onRetry = {},
                    onRefresh = {},
                    onRefreshFailureShown = {},
                    launchEmail = {},
                )
            }
        }
    }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun categoriesStartCollapsedAndExpand() {
        setContent()

        rule.onNodeWithText("Frequently Asked Questions").assertExists()
        rule.onNodeWithText("Account").assertExists().assert(stateDescription("Collapsed"))
        rule.onNodeWithText("Calls").assertExists()
        rule.onNodeWithText("Payments").assertExists()
        rule.onNodeWithText("How do I reset my password?").assertDoesNotExist()

        rule.onNodeWithText("Account").performClick()

        rule.onNodeWithText("How do I reset my password?").assertExists()
        rule.onNodeWithText("Use Settings.").assertExists()
        rule.onNodeWithText("Account").assert(stateDescription("Expanded"))

        rule.onNodeWithText("Account").performClick()

        rule.onNodeWithText("How do I reset my password?").assertDoesNotExist()
        rule.onNodeWithText("Account").assert(stateDescription("Collapsed"))
    }

    @Test
    fun missingSnippetIsOmitted() {
        setContent()

        rule.onNodeWithText("Calls").performClick()

        rule.onNodeWithText("Why did my call drop?").assertExists()
        rule.onNodeWithText("null").assertDoesNotExist()
    }
}
