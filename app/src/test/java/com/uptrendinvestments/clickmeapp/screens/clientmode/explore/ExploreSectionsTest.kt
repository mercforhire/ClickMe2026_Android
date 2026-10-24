package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.components.RATING_STAR_EMPTY_TAG
import com.uptrendinvestments.clickmeapp.components.RATING_STAR_FILLED_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreCategoriesSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreExpertsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreFeaturedTopicsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreHeroHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreTodaysSessionsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_TIME_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    // 3 PM today in the device zone, which the card formats in.
    private val threePm: Instant =
        LocalDate
            .of(2026, 10, 21)
            .atTime(15, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()

    private val ada =
        TodaysSessionUi(
            bookingId = "b1",
            expertName = "Ada Lovelace",
            expertAvatarUrl = null,
            topic = null,
            startTime = threePm,
            endTime = threePm.plusSeconds(3600),
        )

    private fun sessionTimeText(): String =
        rule
            // The clickable card merges its descendants, so read the time text from the unmerged tree.
            .onNodeWithTag(TODAYS_SESSION_TIME_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("")
            // ICU may put a narrow or regular no-break space before AM/PM.
            .replace('\u202F', ' ')
            .replace('\u00A0', ' ')

    private fun setTime1224(value: String?) {
        Settings.System.putString(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.System.TIME_12_24,
            value,
        )
    }

    @Test
    fun todaysSessions_showsCardWithFallbackTopicAndDisabledJoin() {
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), onSessionClick = {}) }
        rule.onNodeWithText("Today's Sessions").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Consultation").assertExists()
        assertEquals("3:00 PM – 4:00 PM", sessionTimeText())
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsNotEnabled()
    }

    @Test
    fun todaysSessions_honorsTheDevice24HourSettingOverTheLocaleDefault() {
        // en-US defaults to 12 hour; the user's 24 hour setting must still win.
        setTime1224("24")
        try {
            show { ExploreTodaysSessionsSection(sessions = listOf(ada), onSessionClick = {}) }
            assertEquals("15:00 – 16:00", sessionTimeText())
        } finally {
            setTime1224(null)
        }
    }

    @Test
    fun todaysSessions_empty_rendersNothing() {
        show { ExploreTodaysSessionsSection(sessions = emptyList(), onSessionClick = {}) }
        assertEquals(false, exists("Today's Sessions"))
    }

    @Test
    fun todaysSessions_tappingTheCardReportsTheBookingIdOnce() {
        val clicked = mutableListOf<String>()
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), onSessionClick = { clicked += it }) }
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(listOf("b1"), clicked)
    }

    @Test
    fun todaysSessions_cardExposesAViewBookingDetailsClickAction() {
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), onSessionClick = {}) }
        val hasLabel =
            SemanticsMatcher("OnClick label is View booking details") { node ->
                node.config.getOrNull(SemanticsActions.OnClick)?.label == "View booking details"
            }
        rule.onNode(hasClickAction() and hasText("Ada Lovelace")).assert(hasLabel)
    }

    @Test
    fun todaysSessions_joinStaysDisabledAndTappingItDoesNotOpenTheBooking() {
        val clicked = mutableListOf<String>()
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), onSessionClick = { clicked += it }) }
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsNotEnabled().performClick()
        assertEquals(emptyList<String>(), clicked)
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

    // Featured Topics

    private val t1 =
        FeaturedTopicUi("t1", "Intro to ML", PriceDisplay.Paid("$50.00"), "e1", "Ada Lovelace", null)
    private val t2 = FeaturedTopicUi("t2", "Career chat", PriceDisplay.Free, "e2", null, null)

    private fun showFeatured(
        state: SectionState<List<FeaturedTopicUi>>,
        onTopicClick: (FeaturedTopicUi) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        show {
            ExploreFeaturedTopicsSection(state = state, onTopicClick = onTopicClick, onRetry = onRetry)
        }
    }

    @Test
    fun featured_loaded_showsCardsPriceAndFallbackExpert() {
        val clicked = mutableListOf<FeaturedTopicUi>()
        showFeatured(SectionState.Loaded(listOf(t1, t2)), onTopicClick = { clicked += it })
        rule.onNodeWithText("Featured Topics").assertExists()
        rule.onNodeWithText("$50.00").assertExists()
        rule.onNodeWithText("Free").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Expert").assertExists()
        assertEquals(false, exists("See all"))
        rule.onNodeWithText("Intro to ML").performClick()
        assertEquals(listOf(t1), clicked)
    }

    @Test
    fun featured_emptyAndLoading_renderNothing() {
        showFeatured(SectionState.Loaded(emptyList()))
        assertEquals(false, exists("Featured Topics"))
    }

    @Test
    fun featured_loading_rendersNothing() {
        showFeatured(SectionState.Loading)
        assertEquals(false, exists("Featured Topics"))
    }

    @Test
    fun featured_error_keepsHeadingAndRetries() {
        var retries = 0
        showFeatured(SectionState.Error(FieldError.Text("Nope")), onRetry = { retries++ })
        rule.onNodeWithText("Featured Topics").assertExists()
        rule.onNodeWithText("Nope").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    // Recommended Experts

    private val adaExpert =
        ExpertCardUi(
            expertId = "e1",
            name = "Ada Lovelace",
            title = "Engineer",
            imageUrl = null,
            rating = 4.5,
            reviewCount = 12,
            tags = listOf("AI", "Math", "Stats", "Ops"),
        )

    private fun showExperts(
        state: SectionState<List<ExpertCardUi>>,
        hasFilter: Boolean = false,
        onExpertClick: (ExpertCardUi) -> Unit = {},
        onClearFilter: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        show {
            ExploreExpertsSection(
                state = state,
                hasFilter = hasFilter,
                onExpertClick = onExpertClick,
                onClearFilter = onClearFilter,
                onRetry = onRetry,
            )
        }
    }

    private fun countWithTag(tag: String) =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun experts_loaded_showsCardDetailsAndCapsTagsAtThree() {
        val clicked = mutableListOf<ExpertCardUi>()
        showExperts(SectionState.Loaded(listOf(adaExpert)), onExpertClick = { clicked += it })
        rule.onNodeWithText("Recommended Experts").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("4.5").assertExists()
        rule.onNodeWithText("· 12 reviews").assertExists()
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("AI").assertExists()
        rule.onNodeWithText("Math").assertExists()
        rule.onNodeWithText("Stats").assertExists()
        assertEquals(false, exists("Ops"))
        assertEquals(false, exists("See all"))
        val favorite =
            SemanticsMatcher("content description contains avorite") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.contains("avorite") } == true
            }
        assertEquals(0, rule.onAllNodes(favorite, useUnmergedTree = true).fetchSemanticsNodes().size)
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(listOf(adaExpert), clicked)
    }

    @Test
    fun experts_noReviews_showsMessageWithoutStars_andOmitsBlankTitleAndTags() {
        val grace = adaExpert.copy(expertId = "e2", name = "Grace", title = " ", reviewCount = 0, tags = emptyList())
        showExperts(SectionState.Loaded(listOf(grace)))
        rule.onNodeWithText("No reviews yet").assertExists()
        assertEquals(0, countWithTag(RATING_STAR_FILLED_TAG))
        assertEquals(0, countWithTag(RATING_STAR_EMPTY_TAG))
        rule.onNodeWithText("Grace").assert(hasContentDescription("Grace. No reviews yet"))
    }

    @Test
    fun experts_emptyWithFilter_showsClearFilter() {
        var cleared = 0
        showExperts(SectionState.Loaded(emptyList()), hasFilter = true, onClearFilter = { cleared++ })
        rule.onNodeWithText("No experts in this category yet").assertExists()
        rule.onNodeWithText("Clear filter").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun experts_emptyWithoutFilter_hasNoAction() {
        showExperts(SectionState.Loaded(emptyList()), hasFilter = false)
        rule.onNodeWithText("No experts available yet").assertExists()
        assertEquals(false, exists("Clear filter"))
    }

    @Test
    fun experts_loading_showsLoadingNode() {
        showExperts(SectionState.Loading)
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithText("Recommended Experts").assertExists()
    }

    @Test
    fun experts_error_keepsHeadingAndRetries() {
        var retries = 0
        showExperts(SectionState.Error(FieldError.Text("Down")), onRetry = { retries++ })
        rule.onNodeWithText("Recommended Experts").assertExists()
        rule.onNodeWithText("Down").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
