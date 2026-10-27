package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.BOOK_SESSION_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.EXPERT_PROFILE_MESSAGE_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileAboutCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileActionButtons
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileActionButtonsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileExpertiseSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileHero
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileReviewsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileStatsBar
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileTopicsCard
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val ButtonBarHeight = 56.dp

@RunWith(RobolectricTestRunner::class)
class ExpertProfileScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun setHero(
        rating: HeroRating,
        title: String = "Engineer",
        isTitlePending: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileHero(
                    name = "Ada Lovelace",
                    title = title,
                    isTitlePending = isTitlePending,
                    imageUrl = null,
                    rating = rating,
                )
            }
        }
    }

    @Test
    fun heroShowsNameTitleAndRatingWithMergedDescription() {
        setHero(HeroRating.Shown(4.8, 7))

        rule
            .onNodeWithText("Ada Lovelace")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("4.8", substring = true).assertExists()
        rule.onNodeWithText("(7 reviews)", substring = true).assertExists()
        rule.onNodeWithContentDescription("Rated 4.8 out of 5 from 7 reviews").assertExists()
    }

    @Test
    fun heroNoReviewsShowsOnlyNoReviewsYet() {
        setHero(HeroRating.NoReviews)

        rule.onNodeWithText("No reviews yet").assertExists()
        rule.onAllNodesWithText("—").assertCountEquals(0)
    }

    @Test
    fun heroHiddenRatingShowsNeitherLine() {
        setHero(HeroRating.Hidden)

        rule.onAllNodesWithText("No reviews yet").assertCountEquals(0)
        rule.onAllNodesWithText("(", substring = true).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(0)
    }

    @Test
    fun heroPendingRatingShowsLoadingNode() {
        setHero(HeroRating.Pending)

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithText("No reviews yet").assertCountEquals(0)
    }

    @Test
    fun heroPendingTitleShowsSkeletonInsteadOfTitle() {
        setHero(HeroRating.Hidden, title = "", isTitlePending = true)

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun heroHasNoOnlineIndicator() {
        setHero(HeroRating.Shown(4.8, 7))

        rule.onAllNodesWithContentDescription("online", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun statsBarShowsExperienceAndBookings() {
        rule.setContent { ClickMeTheme { ExpertProfileStatsBar(experienceYears = 7, totalBookings = 3) } }

        rule.onNodeWithText("Experience").assertExists()
        rule.onNodeWithText("7 yrs").assertExists()
        rule.onNodeWithText("Bookings").assertExists()
        rule.onNodeWithText("3").assertExists()
    }

    @Test
    fun statsBarDropsExperienceWhenUnknown() {
        rule.setContent { ClickMeTheme { ExpertProfileStatsBar(experienceYears = null, totalBookings = 3) } }

        rule.onAllNodesWithText("Experience").assertCountEquals(0)
        rule.onNodeWithText("Bookings").assertExists()
        rule.onNodeWithText("3").assertExists()
    }

    @Test
    fun bookableShowsEnabledBookSessionThatCallsBack() {
        var booked = 0
        rule.setContent {
            ClickMeTheme { ExpertProfileActionButtons(isBookable = true, onBookSession = { booked++ }) }
        }

        rule.onNodeWithTag(BOOK_SESSION_TAG).assertIsEnabled()
        rule.onNodeWithText("Book Session").assertExists()
        rule.onNodeWithTag(BOOK_SESSION_TAG).performClick()
        assertEquals(1, booked)
    }

    @Test
    fun notBookableShowsDisabledPillAndNoBookSession() {
        rule.setContent {
            ClickMeTheme { ExpertProfileActionButtons(isBookable = false, onBookSession = {}) }
        }

        rule.onNodeWithText("Not accepting bookings").assertIsNotEnabled()
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
    }

    @Test
    fun aboutCardShowsHeadingAndBio() {
        rule.setContent { ClickMeTheme { ExpertProfileAboutCard(bio = "Hi there") } }

        rule.onNodeWithText("About").assertExists()
        rule.onNodeWithText("Hi there").assertExists()
    }

    @Test
    fun expertiseSectionShowsHeadingAndTags() {
        rule.setContent { ClickMeTheme { ExpertProfileExpertiseSection(tags = listOf("AI", "Math")) } }

        rule.onNodeWithText("Expertise").assertExists()
        rule.onNodeWithText("AI").assertExists()
        rule.onNodeWithText("Math").assertExists()
    }

    @Test
    fun topicsCardShowsPricesAndSubtitlesWithoutClickActions() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileTopicsCard(
                    topics =
                        listOf(
                            TopicRowUi("1", "Free chat", 30, "Intro", PriceDisplay.Free),
                            TopicRowUi("2", "Deep dive", 60, null, PriceDisplay.Paid("$50.00")),
                            TopicRowUi("3", "Mystery", null, null, PriceDisplay.Unknown),
                        ),
                )
            }
        }

        rule.onNodeWithText("Topics of Discussion").assertExists()
        rule.onNodeWithText("Free").assertExists()
        rule.onNodeWithText("$50.00").assertExists()
        rule.onNodeWithText("—").assertExists()
        rule.onNodeWithText("30 mins • Intro").assertHasNoClickAction()
        rule.onNodeWithText("60 mins").assertExists()
    }

    @Test
    fun reviewsCardShowsThreeRowsAnonymousFallbackAndNoSeeAll() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileReviewsCard(
                    reviews =
                        listOf(
                            ReviewRowUi("1", "Grace", 5, "Great talk"),
                            ReviewRowUi("2", null, 4, "   "),
                            ReviewRowUi("3", "Alan", 3, null),
                            ReviewRowUi("4", "Hidden", 5, "Fourth"),
                        ),
                )
            }
        }

        rule.onNodeWithText("Recent Reviews").assertExists()
        rule.onNodeWithText("Grace").assertExists()
        rule.onNodeWithText("Anonymous").assertExists()
        rule.onNodeWithText("Alan").assertExists()
        rule.onAllNodesWithText("Hidden").assertCountEquals(0)
        rule.onAllNodesWithText("Fourth").assertCountEquals(0)
        rule.onAllNodesWithText("   ").assertCountEquals(0)
        rule.onAllNodesWithText("See all", ignoreCase = true).assertCountEquals(0)
    }

    private fun loadedState(
        tags: List<String> = listOf("AI"),
        bio: String? = "Hi",
        topics: List<TopicRowUi> = listOf(TopicRowUi("t1", "Topic one", 30, null, PriceDisplay.Free)),
        reviews: ReviewsState = ReviewsState.Loaded(listOf(ReviewRowUi("r1", "Grace", 5, "Great"))),
        isBookable: Boolean = true,
    ) = ExpertProfileUiState(
        expertId = "e1",
        name = "Ada Lovelace",
        title = "Engineer",
        imageUrl = null,
        tags = tags,
        rating = HeroRating.Shown(4.8, 7),
        details =
            DetailsState.Loaded(
                ProfileDetailsUi(
                    experienceYears = 5,
                    totalBookings = 3,
                    bio = bio,
                    topics = topics,
                    isBookable = isBookable,
                ),
            ),
        reviews = reviews,
    )

    private var backCount = 0
    private var bookCount = 0
    private var retryCount = 0

    private fun setContent(state: ExpertProfileUiState) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = state,
                    onBack = { backCount++ },
                    onBookSession = { bookCount++ },
                    onRetry = { retryCount++ },
                )
            }
        }
    }

    private fun yOf(text: String): Float {
        val node = rule.onNodeWithText(text).fetchSemanticsNode()
        return node.positionInRoot.y
    }

    @Test
    fun loadedContentShowsSectionsInOrderAndBookSessionCallsBack() {
        setContent(loadedState())

        val order =
            listOf(
                "Ada Lovelace",
                "Experience",
                "Book Session",
                "About",
                "Expertise",
                "Topics of Discussion",
            ).map(::yOf)
        assertEquals(order.sorted(), order)
        assertTrue(order.distinct().size == order.size)

        rule
            .onNodeWithText("Book Session")
            .performScrollTo()
            .performClick()
        assertEquals(1, bookCount)

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Recent Reviews"))
        rule.onNodeWithText("Recent Reviews").assertExists()
        rule.onNodeWithText("Grace").assertExists()
    }

    @Test
    fun emptySectionsAreAbsent() {
        setContent(
            loadedState(
                tags = emptyList(),
                bio = "  ",
                topics = emptyList(),
                reviews = ReviewsState.Loaded(emptyList()),
            ),
        )

        rule.onNodeWithText("Book Session").assertExists()
        rule.onAllNodesWithText("Expertise").assertCountEquals(0)
        rule.onAllNodesWithText("Topics of Discussion").assertCountEquals(0)
        rule.onAllNodesWithText("Recent Reviews").assertCountEquals(0)
        rule.onAllNodesWithText("About").assertCountEquals(0)
    }

    @Test
    fun loadingShowsHeroAndSkeletonsWithoutBookSession() {
        setContent(loadedState().copy(details = DetailsState.Loading, reviews = ReviewsState.Loading))

        rule.onNodeWithText("Ada Lovelace").assertExists()
        // One merged "Loading" node for the details skeletons; the reviews skeleton sits below the fold.
        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(1)
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
        rule.onAllNodesWithText("Experience").assertCountEquals(0)
    }

    @Test
    fun unavailableReplacesTheBodyAndGoBackPops() {
        setContent(loadedState().copy(details = DetailsState.Unavailable))

        rule.onNodeWithText("This profile isn't available").assertExists()
        rule.onNodeWithText("This expert may no longer be on ClickMe. Go back and try someone else.").assertExists()
        rule.onAllNodesWithText("Ada Lovelace").assertCountEquals(0)
        rule.onNodeWithContentDescription("Back").assertExists()
        rule.onNodeWithText("Go back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun detailsErrorKeepsHeroAndRetries() {
        setContent(loadedState().copy(details = DetailsState.Error))

        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Something went wrong").assertExists()
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
        rule.onNodeWithText("Retry connection").performScrollTo().performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun backArrowPops() {
        setContent(loadedState())

        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    private var messageCount = 0

    private fun setMessageContent(
        state: ExpertProfileUiState,
        showMessage: Boolean,
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = state,
                    onBack = {},
                    onBookSession = {},
                    onRetry = {},
                    showMessage = showMessage,
                    onMessage = { messageCount++ },
                )
            }
        }
    }

    @Test
    fun messageButtonCallsOnMessage() {
        setMessageContent(loadedState(), showMessage = true)

        rule.onNodeWithTag(EXPERT_PROFILE_MESSAGE_TAG).performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Message").performClick()
        assertEquals(1, messageCount)
    }

    @Test
    fun messageStaysEnabledWhenNotBookable() {
        setMessageContent(loadedState(isBookable = false), showMessage = true)

        rule.onNodeWithText("Not accepting bookings").assertIsNotEnabled()
        rule.onNodeWithTag(EXPERT_PROFILE_MESSAGE_TAG).assertIsEnabled()
    }

    @Test
    fun ownProfileHidesMessage() {
        setMessageContent(loadedState(), showMessage = false)

        rule.onNodeWithText("Book Session").assertExists()
        rule.onAllNodesWithTag(EXPERT_PROFILE_MESSAGE_TAG).assertCountEquals(0)
    }

    @Test
    fun skeletonShowsTwoBars() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileActionButtonsSkeleton(Modifier.testTag("skeleton"))
            }
        }

        rule
            .onNodeWithTag("skeleton")
            .assertHeightIsAtLeast(ButtonBarHeight * 2 + Spacing.md)
    }

    @Test
    fun deferredControlsNeverAppear() {
        setContent(loadedState())

        rule.onAllNodesWithText("Message").assertCountEquals(0)
        rule.onAllNodesWithText("See all", ignoreCase = true).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("avorite", substring = true).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("More options", substring = true).assertCountEquals(0)
    }

    @Test
    fun notBookableProfileShowsDisabledPill() {
        setContent(loadedState(isBookable = false))

        rule.onNodeWithText("Not accepting bookings").assertIsNotEnabled()
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
    }
}
