package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
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
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RATING_STAR_EMPTY_TAG
import com.uptrendinvestments.clickmeapp.components.RATING_STAR_FILLED_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.EXPLORE_EXPERTS_SEE_ALL_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.EXPLORE_FEATURED_SEE_ALL_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreCategoriesSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreExpertsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreFeaturedTopicsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreHeroHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.ExploreTodaysSessionsSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.STARTING_NOW_BADGE_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.StartingNowBadge
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_TIME_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
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

    // Ten minutes before the session: inside the join window.
    private val soon: Instant = threePm.minusSeconds(600)

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
    fun todaysSessions_showsCardWithFallbackTopicAndEnabledJoin() {
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), now = soon, onSessionClick = {}, onJoin = {}) }
        rule.onNodeWithText("Today's Sessions").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Consultation").assertExists()
        assertEquals("3:00 PM – 4:00 PM", sessionTimeText())
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsEnabled()
    }

    @Test
    fun todaysSessions_honorsTheDevice24HourSettingOverTheLocaleDefault() {
        // en-US defaults to 12 hour; the user's 24 hour setting must still win.
        setTime1224("24")
        try {
            show { ExploreTodaysSessionsSection(sessions = listOf(ada), now = soon, onSessionClick = {}, onJoin = {}) }
            assertEquals("15:00 – 16:00", sessionTimeText())
        } finally {
            setTime1224(null)
        }
    }

    @Test
    fun todaysSessions_empty_rendersNothing() {
        show { ExploreTodaysSessionsSection(sessions = emptyList(), now = soon, onSessionClick = {}, onJoin = {}) }
        assertEquals(false, exists("Today's Sessions"))
    }

    @Test
    fun todaysSessions_tappingTheCardReportsTheBookingIdOnce() {
        val clicked = mutableListOf<String>()
        show {
            ExploreTodaysSessionsSection(
                sessions = listOf(ada),
                now = soon,
                onSessionClick = { clicked += it },
                onJoin = {},
            )
        }
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(listOf("b1"), clicked)
    }

    @Test
    fun todaysSessions_cardExposesAViewBookingDetailsClickAction() {
        show { ExploreTodaysSessionsSection(sessions = listOf(ada), now = soon, onSessionClick = {}, onJoin = {}) }
        val hasLabel =
            SemanticsMatcher("OnClick label is View booking details") { node ->
                node.config.getOrNull(SemanticsActions.OnClick)?.label == "View booking details"
            }
        rule.onNode(hasClickAction() and hasText("Ada Lovelace")).assert(hasLabel)
    }

    @Test
    fun todaysSessions_joinTapStartsTheCallAndDoesNotOpenTheBooking() {
        val clicked = mutableListOf<String>()
        val joined = mutableListOf<TodaysSessionUi>()
        show {
            ExploreTodaysSessionsSection(
                sessions = listOf(ada),
                now = soon,
                onSessionClick = { clicked += it },
                onJoin = { joined += it },
            )
        }
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(listOf(ada), joined)
        assertEquals(emptyList<String>(), clicked)
    }

    @Test
    fun todaysSessions_joinIsDisabledOutsideTheWindowAndNoBadge() {
        val joined = mutableListOf<TodaysSessionUi>()
        show {
            ExploreTodaysSessionsSection(
                sessions = listOf(ada),
                now = threePm.minusSeconds(3 * 3600L),
                onSessionClick = {},
                onJoin = { joined += it },
            )
        }
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsNotEnabled().performClick()
        assertEquals(emptyList<TodaysSessionUi>(), joined)
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assert(
            SemanticsMatcher("stateDescription explains why") {
                it.config.getOrNull(SemanticsProperties.StateDescription) == "Available closer to the session start"
            },
        )
        rule.onAllNodesWithTag(STARTING_NOW_BADGE_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun todaysSessions_startedHalfAnHourAgoShowsTheBadgeAndEnabledJoin() {
        show {
            ExploreTodaysSessionsSection(
                sessions = listOf(ada),
                now = threePm.plusSeconds(1800),
                onSessionClick = {},
                onJoin = {},
            )
        }
        rule.onNodeWithText("Starting now").assertExists()
        rule.onNodeWithTag(STARTING_NOW_BADGE_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsEnabled()
    }

    @Test
    fun todaysSessions_pendingApprovalNeverEnablesJoin() {
        show {
            ExploreTodaysSessionsSection(
                sessions = listOf(ada.copy(status = "pending_approval")),
                now = soon,
                onSessionClick = {},
                onJoin = {},
            )
        }
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).assertIsNotEnabled()
    }

    @Test
    fun startingNowBadge_showsSentenceCaseText() {
        show { StartingNowBadge() }
        rule.onNodeWithText("Starting now").assertExists()
        rule.onNodeWithTag(STARTING_NOW_BADGE_TAG).assertExists()
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
        onSeeAll: (() -> Unit)? = null,
    ) {
        show {
            ExploreFeaturedTopicsSection(
                state = state,
                onTopicClick = onTopicClick,
                onRetry = onRetry,
                onSeeAll = onSeeAll,
            )
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

    private fun assertSeeAllClickedOnce(
        tag: String,
        clicks: () -> Int,
    ) {
        val action = rule.onNodeWithTag(tag)
        action.assert(hasText("See all"))
        action.assertHasClickAction()
        action.performClick()
        assertEquals(1, clicks())
    }

    @Test
    fun featured_seeAll_loadedWithTopicsCallsOnSeeAll() {
        var seeAll = 0
        showFeatured(SectionState.Loaded(listOf(t1, t2)), onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_FEATURED_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun featured_seeAll_errorStateCallsOnSeeAll() {
        var seeAll = 0
        showFeatured(SectionState.Error(FieldError.Res(R.string.error_state_body)), onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_FEATURED_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun featured_seeAll_loadingAndEmptyRenderNoHeadingAndNoAction() {
        showFeatured(SectionState.Loading, onSeeAll = {})
        assertEquals(false, exists("Featured Topics"))
        assertEquals(0, countWithTag(EXPLORE_FEATURED_SEE_ALL_TAG))
    }

    @Test
    fun featured_seeAll_emptyRendersNoHeadingAndNoAction() {
        showFeatured(SectionState.Loaded(emptyList()), onSeeAll = {})
        assertEquals(false, exists("Featured Topics"))
        assertEquals(0, countWithTag(EXPLORE_FEATURED_SEE_ALL_TAG))
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
        onSeeAll: (() -> Unit)? = null,
    ) {
        show {
            ExploreExpertsSection(
                state = state,
                hasFilter = hasFilter,
                onExpertClick = onExpertClick,
                onClearFilter = onClearFilter,
                onRetry = onRetry,
                onSeeAll = onSeeAll,
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

    @Test
    fun experts_seeAll_loadingCallsOnSeeAll() {
        var seeAll = 0
        showExperts(SectionState.Loading, onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_EXPERTS_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun experts_seeAll_emptyCallsOnSeeAll() {
        var seeAll = 0
        showExperts(SectionState.Loaded(emptyList()), onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_EXPERTS_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun experts_seeAll_loadedCallsOnSeeAll() {
        var seeAll = 0
        showExperts(SectionState.Loaded(listOf(adaExpert)), onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_EXPERTS_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun experts_seeAll_errorCallsOnSeeAll() {
        var seeAll = 0
        showExperts(SectionState.Error(FieldError.Res(R.string.error_state_body)), onSeeAll = { seeAll++ })
        assertSeeAllClickedOnce(EXPLORE_EXPERTS_SEE_ALL_TAG) { seeAll }
    }

    @Test
    fun experts_withoutOnSeeAll_showsNoAction() {
        showExperts(SectionState.Loaded(listOf(adaExpert)))
        assertEquals(0, countWithTag(EXPLORE_EXPERTS_SEE_ALL_TAG))
        assertEquals(false, exists("See all"))
    }

    @Test
    fun seeAll_meetsTouchTargetAndIsNamed() {
        showExperts(SectionState.Loaded(listOf(adaExpert)), onSeeAll = {})
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}
