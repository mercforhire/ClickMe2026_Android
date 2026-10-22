package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreCategoriesSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreHeroHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreTodaysSessionsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExploreSectionsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    // Hero

    @Test
    fun hero_showsTitleAndSubtitle() {
        show { ExploreHeroHeader() }
        rule.onNodeWithText("Find your next expert").assertExists()
        rule.onNodeWithText("Connect with the top 1% of industry leaders.").assertExists()
    }

    // Today's Sessions

    private val ada =
        TodaysSessionUi(
            bookingId = "b1",
            expertName = "Ada Lovelace",
            expertAvatarUrl = null,
            topic = null,
            timeRange = "3:00 PM – 4:00 PM",
        )

    @Test
    fun todaysSessions_showsCardWithFallbackTopicAndDisabledJoin() {
        show { ExploreTodaysSessionsSection(sessions = listOf(ada)) }
        rule.onNodeWithText("Today's Sessions").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists().assertHasNoClickAction()
        rule.onNodeWithText("Consultation").assertExists()
        rule.onNodeWithText("3:00 PM – 4:00 PM").assertExists()
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsNotEnabled()
    }

    @Test
    fun todaysSessions_empty_rendersNothing() {
        show { ExploreTodaysSessionsSection(sessions = emptyList()) }
        assertEquals(false, exists("Today's Sessions"))
    }

    // Trending Categories

    private val chips =
        listOf(
            CategoryChipUi("ai", "AI"),
            CategoryChipUi("design", "Design"),
            CategoryChipUi("tech", null),
        )

    private fun showCategories(
        state: SectionState<List<CategoryChipUi>>,
        selection: Set<String> = emptySet(),
        onChipTap: (String) -> Unit = {},
        onViewAll: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        show {
            ExploreCategoriesSection(
                state = state,
                selection = selection,
                onChipTap = onChipTap,
                onViewAll = onViewAll,
                onRetry = onRetry,
            )
        }
    }

    @Test
    fun categories_selectedCardIsOnAndOthersOff_nullNameFallsBack() {
        showCategories(SectionState.Loaded(chips), selection = setOf("ai"))
        rule.onNodeWithText("AI").assertIsOn()
        rule.onNodeWithText("Design").assertIsOff()
        rule.onNodeWithText("Category").assertExists()
        rule.onNodeWithText("View all (1)").assertExists()
    }

    @Test
    fun categories_emptySelection_actionReadsViewAll() {
        showCategories(SectionState.Loaded(chips))
        rule.onNodeWithText("View all").assertExists()
    }

    @Test
    fun categories_tapsReportSlugAndViewAll() {
        val taps = mutableListOf<String>()
        var viewAll = 0
        showCategories(SectionState.Loaded(chips), onChipTap = { taps += it }, onViewAll = { viewAll++ })
        rule.onNodeWithText("Design").performClick()
        rule.onNodeWithText("View all").performClick()
        assertEquals(listOf("design"), taps)
        assertEquals(1, viewAll)
    }

    @Test
    fun categories_loading_showsLoadingNodeAndNoChips() {
        showCategories(SectionState.Loading)
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertEquals(false, exists("AI"))
        rule.onNodeWithText("Trending Categories").assertExists()
    }

    @Test
    fun categories_error_keepsHeadingAndRetries() {
        var retries = 0
        showCategories(SectionState.Error(FieldError.Text("Boom")), onRetry = { retries++ })
        rule.onNodeWithText("Trending Categories").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
