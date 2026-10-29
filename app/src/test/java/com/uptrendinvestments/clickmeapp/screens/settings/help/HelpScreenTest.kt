package com.uptrendinvestments.clickmeapp.screens.settings.help

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.faqArticle
import com.uptrendinvestments.clickmeapp.testing.faqCategory
import com.uptrendinvestments.clickmeapp.testing.faqsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HelpScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

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

    private val retries = mutableListOf<Unit>()
    private val shown = mutableListOf<Unit>()

    private fun setContent(
        state: HelpUiState = HelpUiState(load = HelpLoad.Loaded(categories)),
        launchEmail: (Intent) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                HelpContent(
                    state = state,
                    onBack = {},
                    onRetry = { retries += Unit },
                    onRefresh = {},
                    onRefreshFailureShown = { shown += Unit },
                    launchEmail = launchEmail,
                )
            }
        }
    }

    private fun loaded(vararg list: FaqCategory) = HelpUiState(load = HelpLoad.Loaded(list.toList()))

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

    @Test
    fun emailUsStartsSendTo() {
        api.faqsHandler = { faqsPayload() }
        rule.setContent { ClickMeTheme { HelpScreen(viewModel = HelpViewModel(HelpRepository(api)), onBack = {}) } }

        rule.onNodeWithTag("help_email_us").performClick()

        val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:support@clickme.app", intent.dataString)
    }

    @Test
    fun noMailAppShowsAddress() {
        setContent(launchEmail = { throw ActivityNotFoundException() })

        rule.onNodeWithText("Email Us").performClick()

        rule
            .onNodeWithText("No email app found. Write to support@clickme.app from any mail app.")
            .assertExists()
    }

    @Test
    fun failedLoadKeepsContactUs() {
        api.faqsHandler = { throw api.httpError(500, """{"status":"error","message":"Down"}""") }
        rule.setContent { ClickMeTheme { HelpScreen(viewModel = HelpViewModel(HelpRepository(api)), onBack = {}) } }

        rule.onNodeWithText("Couldn't load help").assertExists()
        rule.onNodeWithText("Down").assertExists()
        rule.onNodeWithText("Retry").assertExists()
        rule.onNodeWithText("Email Us").assertExists()

        api.faqsHandler = { faqsPayload(categories[0]) }
        rule.onNodeWithText("Retry").performClick()

        assertEquals(2, api.faqsCalls)
        rule.onNodeWithText("Account").assertExists()
    }

    @Test
    fun networkFailureShowsConnectionCopy() {
        setContent(HelpUiState(load = HelpLoad.Failed(FieldError.Res(R.string.login_error_network))))

        rule.onNodeWithText("Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries.size)
        rule.onNodeWithText("Email Us").assertExists()
    }

    @Test
    fun emptyListShowsOnlyContactUs() {
        setContent(loaded())

        rule.onNodeWithText("Frequently Asked Questions").assertDoesNotExist()
        rule.onNodeWithText("Contact Us").assertExists()
        rule.onNodeWithText("Email Us").assertExists()
    }

    @Test
    fun refreshFailureShowsSnackbarAndConsumesFlag() {
        setContent(HelpUiState(load = HelpLoad.Loaded(categories), refreshFailed = true))

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, shown.size)
        rule.onNodeWithText("Account").assertExists()
    }

    @Test
    fun loadingShowsSkeletonWithLoadingDescription() {
        setContent(HelpUiState())

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithText("Frequently Asked Questions").assertDoesNotExist()
    }
}
