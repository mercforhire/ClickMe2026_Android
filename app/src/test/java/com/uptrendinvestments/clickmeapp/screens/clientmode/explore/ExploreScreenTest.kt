package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.TODAYS_SESSION_JOIN_TAG
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

// A tall window composes every lazy item at once, so vertical positions can be compared.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ExploreScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val session =
        TodaysSessionUi(
            "b1",
            "Grace Hopper",
            null,
            "Compilers",
            Instant.parse("2026-10-21T19:00:00Z"),
            Instant.parse("2026-10-21T20:00:00Z"),
        )
    private val chip = CategoryChipUi(slug = "tech", name = "Technology")
    private val topic =
        FeaturedTopicUi("t1", "Intro to ML", PriceDisplay.Paid("$50.00"), "e2", null, "v")
    private val expert =
        ExpertCardUi("e1", "Ada Lovelace", "Engineer", "u", 4.5, 12, listOf("AI"))

    private val fullState =
        ExploreUiState(
            todaysSessions = listOf(session),
            categories = SectionState.Loaded(listOf(chip)),
            featuredTopics = SectionState.Loaded(listOf(topic)),
            experts = SectionState.Loaded(listOf(expert)),
        )

    private var expertClicked: ExpertCardUi? = null
    private var topicClicked: FeaturedTopicUi? = null
    private var viewAllCount = 0
    private var chipTapped: String? = null
    private var consumedCount = 0
    private var sessionClicked: String? = null
    private var sessionJoined: TodaysSessionUi? = null

    private fun show(state: ExploreUiState) {
        rule.setContent {
            ClickMeTheme {
                ExploreContent(
                    state = state,
                    avatarUrl = null,
                    onAvatarClick = {},
                    onRefresh = {},
                    onRetry = {},
                    onChipTap = { chipTapped = it },
                    onViewAll = { viewAllCount++ },
                    onClearFilter = {},
                    onExpertClick = { expertClicked = it },
                    onFeaturedTopicClick = { topicClicked = it },
                    onSessionClick = { sessionClicked = it },
                    joinNow = Instant.parse("2026-10-21T18:50:00Z"),
                    onJoinSession = { sessionJoined = it },
                    onRefreshFailureConsumed = { consumedCount++ },
                )
            }
        }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun top(text: String): Float {
        val bounds = rule.onNodeWithText(text).getUnclippedBoundsInRoot()
        return bounds.top.value
    }

    @Test
    fun mapper_expertCardCarriesEveryField() {
        val key = expert.toProfileKey("Expert")
        assertEquals(ExpertProfileKey("e1", "Ada Lovelace", "Engineer", "u", 4.5, 12, listOf("AI")), key)
    }

    @Test
    fun mapper_expertCardNullNameUsesFallback() {
        assertEquals("Expert", expert.copy(name = null).toProfileKey("Expert").name)
    }

    @Test
    fun mapper_featuredTopicCarriesOnlyIdNameAndImage() {
        val key = topic.toProfileKey("Expert")
        assertEquals(ExpertProfileKey("e2", "Expert", "", "v", null, null, emptyList()), key)
    }

    @Test
    fun todaysSessionCardTapForwardsTheBookingId() {
        show(fullState)
        rule.onNodeWithText("Grace Hopper").performClick()
        assertEquals("b1", sessionClicked)
    }

    @Test
    fun todaysSessionJoinForwardsTheSessionAndDoesNotOpenTheBooking() {
        show(fullState)
        rule.onNodeWithTag(TODAYS_SESSION_JOIN_TAG).performClick()
        assertEquals(session, sessionJoined)
        assertEquals(null, sessionClicked)
    }

    @Test
    fun showsTitleAndAvatarButton() {
        show(fullState)
        rule.onNodeWithContentDescription("Your profile").assertExists()
        // The bar title also matches the tab label elsewhere, so check the app bar through the hero instead.
        rule.onNodeWithText("Find your next expert").assertExists()
        assertTrue(exists("Explore"))
    }

    @Test
    fun sectionsAppearInIosOrder() {
        show(fullState)
        rule.onNodeWithTag(EXPLORE_LIST_TAG).assertExists()
        val order =
            listOf(
                "Find your next expert",
                "Today's Sessions",
                "Trending Categories",
                "Featured Topics",
                "Recommended Experts",
            ).map { top(it) }
        assertEquals(order.sorted(), order)
        assertEquals(order.size, order.toSet().size)
    }

    @Test
    fun emptyOptionalSectionsAreHidden() {
        show(
            fullState.copy(
                todaysSessions = emptyList(),
                featuredTopics = SectionState.Loaded(emptyList()),
            ),
        )
        assertFalse(exists("Today's Sessions"))
        assertFalse(exists("Featured Topics"))
        assertTrue(exists("Trending Categories"))
        assertTrue(exists("Recommended Experts"))
    }

    @Test
    fun expertCardClickReportsExpert() {
        show(fullState)
        rule.onNodeWithText("Ada Lovelace", substring = true).performClick()
        assertEquals(expert, expertClicked)
    }

    @Test
    fun featuredCardClickReportsTopic() {
        show(fullState)
        rule.onNodeWithText("Intro to ML").performClick()
        assertEquals(topic, topicClicked)
    }

    @Test
    fun viewAllClickIsReported() {
        show(fullState)
        rule.onNodeWithText("View all").performClick()
        assertEquals(1, viewAllCount)
    }

    @Test
    fun chipClickIsReported() {
        show(fullState)
        rule.onNodeWithText("Technology").performClick()
        assertEquals("tech", chipTapped)
    }

    @Test
    fun refreshFailureShowsSnackbarAndIsConsumedOnce() {
        show(fullState.copy(refreshFailed = true))
        rule.waitForIdle()
        rule.onNodeWithText("Couldn't refresh. Pull down to try again.").assertExists()
        assertEquals(1, consumedCount)
    }

    @Test
    fun noSeeAllAnywhere() {
        show(fullState)
        assertFalse(exists("See all"))
    }
}
