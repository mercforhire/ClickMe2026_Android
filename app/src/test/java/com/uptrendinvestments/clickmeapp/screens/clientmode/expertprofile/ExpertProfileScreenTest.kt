package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
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
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.ReviewItemDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewSummaryDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewerDto
import com.uptrendinvestments.clickmeapp.model.responses.StatusOnlyResponse
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
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
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.reportData
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.testing.toggleFavorite
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            ClickMeTheme {
                ExpertProfileActionButtons(isBookable = true, onBookSession = { booked++ }, expertName = "Ada Lovelace")
            }
        }

        rule.onNodeWithTag(BOOK_SESSION_TAG).assertIsEnabled()
        rule.onNodeWithText("Book Session").assertExists()
        rule.onNodeWithTag(BOOK_SESSION_TAG).performClick()
        assertEquals(1, booked)
    }

    @Test
    fun notBookableShowsDisabledPillAndNoBookSession() {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileActionButtons(
                    isBookable = false,
                    onBookSession = {},
                    expertName = "Ada Lovelace",
                )
            }
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
    fun reviewsCardShowsThreeRowsAnonymousFallbackAndSeeAll() {
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
                    onSeeAll = {},
                )
            }
        }

        rule
            .onNodeWithText("Recent Reviews")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Grace").assertExists()
        rule.onNodeWithText("Anonymous").assertExists()
        rule.onNodeWithText("Alan").assertExists()
        rule.onAllNodesWithText("Hidden").assertCountEquals(0)
        rule.onAllNodesWithText("Fourth").assertCountEquals(0)
        rule.onAllNodesWithText("   ").assertCountEquals(0)
        rule.onAllNodesWithText("See all").assertCountEquals(1)
        rule
            .onNodeWithText("See all")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun seeAllInvokesCallbackWithTheShownName() {
        val names = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = loadedState(),
                    onBack = {},
                    onBookSession = {},
                    onRetry = {},
                    onSeeAllReviews = { names += it },
                )
            }
        }

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("See all"))
        rule.onNodeWithText("See all").performClick()
        assertEquals(listOf("Ada Lovelace"), names)
    }

    @Test
    fun zeroReviewsShowsHeaderAndSeeAllOnly() {
        setContent(loadedState(reviews = ReviewsState.Loaded(emptyList())))

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Recent Reviews"))
        rule.onNodeWithText("Recent Reviews").assertExists()
        rule.onNodeWithText("See all").assertExists()
        rule.onAllNodesWithText("Anonymous").assertCountEquals(0)
    }

    @Test
    fun failedReviewsKeepTheCardHidden() {
        setContent(loadedState(reviews = ReviewsState.Failed))

        rule.onAllNodesWithText("Recent Reviews").assertCountEquals(0)
        rule.onAllNodesWithText("See all").assertCountEquals(0)
    }

    @Test
    fun reviewsLoadingShowsNoSeeAll() {
        setContent(loadedState(reviews = ReviewsState.Loading))

        rule.onAllNodesWithText("See all").assertCountEquals(0)
        rule.onAllNodesWithText("Recent Reviews").assertCountEquals(0)
    }

    @Test
    fun selfViewKeepsSeeAll() {
        setHeartScreen(isSelfView = true) { api ->
            api.expertReviewsHandler = {
                ExpertReviewsPayload(
                    summary = ReviewSummaryDto(overallRating = 5.0, totalReviews = 1),
                    reviews = listOf(ReviewItemDto(id = "r1", reviewer = ReviewerDto(name = "Grace"), rating = 5)),
                )
            }
        }

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("See all"))
        rule.onNodeWithText("See all").assertExists()
        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
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

    private fun setContentWithoutBooking(state: ExpertProfileUiState) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = state,
                    onBack = {},
                    onBookSession = { bookCount++ },
                    onRetry = {},
                    showBookSession = false,
                )
            }
        }
    }

    @Test
    fun selfViewHidesBookSession() {
        setContentWithoutBooking(loadedState())

        rule.onNodeWithText("Experience").assertExists()
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
        rule.onAllNodesWithTag(BOOK_SESSION_TAG).assertCountEquals(0)
        assertEquals(0, bookCount)
    }

    @Test
    fun bookSessionStaysByDefault() {
        setContent(loadedState())

        rule.onAllNodesWithText("Book Session").assertCountEquals(1)
    }

    @Test
    fun selfViewKeepsTheRestOfTheProfile() {
        setContentWithoutBooking(loadedState())

        rule.onAllNodesWithText("Ada Lovelace").assertCountEquals(1)
        rule.onNodeWithText("About").assertExists()
        rule.onNodeWithText("Hi").assertExists()
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
                reviews = ReviewsState.Failed,
            ),
        )

        rule.onNodeWithText("Book Session").assertExists()
        rule.onAllNodesWithText("Expertise").assertCountEquals(0)
        rule.onAllNodesWithText("Topics of Discussion").assertCountEquals(0)
        rule.onAllNodesWithText("About").assertCountEquals(0)
        rule.onAllNodesWithText("Recent Reviews").assertCountEquals(0)
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
    fun skeletonDrawsOnlyAsManyBarsAsTheLoadedLayoutHas() {
        rule.setContent {
            ClickMeTheme {
                Column {
                    ExpertProfileActionButtonsSkeleton(Modifier.testTag("one"), pills = 1)
                    ExpertProfileActionButtonsSkeleton(Modifier.testTag("none"), pills = 0)
                }
            }
        }

        rule.onNodeWithTag("one").assertHeightIsEqualTo(ButtonBarHeight)
        rule.onAllNodesWithTag("none").assertCountEquals(0)
    }

    private var reportCount = 0
    private var blockCount = 0

    private fun setSafetyContent(showSafetyMenu: Boolean) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = loadedState(),
                    onBack = {},
                    onBookSession = {},
                    onRetry = {},
                    showSafetyMenu = showSafetyMenu,
                    onReport = { reportCount++ },
                    onBlock = { blockCount++ },
                )
            }
        }
    }

    @Test
    fun menuOpensReportSheet() {
        setSafetyContent(showSafetyMenu = true)

        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Block Ada Lovelace").assertExists()
        rule.onNodeWithText("Report this profile").performClick()

        assertEquals(1, reportCount)
        assertEquals(0, blockCount)
    }

    @Test
    fun menuBlockItemCallsOnBlock() {
        setSafetyContent(showSafetyMenu = true)

        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Block Ada Lovelace").performClick()

        assertEquals(1, blockCount)
        assertEquals(0, reportCount)
    }

    @Test
    fun hiddenMenuHasNoMoreOptions() {
        setSafetyContent(showSafetyMenu = false)

        rule.onAllNodesWithContentDescription("More options").assertCountEquals(0)
    }

    @Test
    fun deferredControlsNeverAppear() {
        setContent(loadedState())

        rule.onAllNodesWithText("Message").assertCountEquals(0)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("See all"))
        rule.onNodeWithText("See all").assertExists()
        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Remove from saved").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("More options", substring = true).assertCountEquals(0)
    }

    @Test
    fun notBookableProfileShowsDisabledPill() {
        setContent(loadedState(isBookable = false))

        rule.onNodeWithText("Not accepting bookings").assertIsNotEnabled()
        rule.onAllNodesWithText("Book Session").assertCountEquals(0)
    }

    private fun setProfileScreen(
        api: FakeClickMeApi,
        safety: ProfileSafetyViewModel,
    ) {
        api.expertDetailsHandler = { CompletableDeferred<PublicProfileDetailsData>().await() }
        api.expertReviewsHandler = { CompletableDeferred<ExpertReviewsPayload>().await() }
        val profile =
            ExpertProfileViewModel(
                ExpertProfileKey(expertId = "e1", name = "Ada", title = "", imageUrl = null),
                ExpertProfileRepository(api),
                FavoritesRepository(api),
            )
        val entry = testChatEntryViewModel(api)
        rule.setContent {
            ClickMeTheme {
                ExpertProfileScreen(
                    viewModel = profile,
                    onBack = {},
                    onBook = {},
                    chatEntry = entry,
                    safety = safety,
                )
            }
        }
    }

    private fun safetyViewModel(api: FakeClickMeApi) =
        ProfileSafetyViewModel(SavedStateHandle(), ChatsRepository(api), ChatSessionState())

    @Test
    fun reportSuccessShowsThanksSnackbar() {
        val api = FakeClickMeApi()
        api.reportUserHandler = { _, _ -> reportData() }
        val safety = safetyViewModel(api)
        setProfileScreen(api, safety)

        rule.runOnIdle {
            safety.selectReason(UserReportReason.Spam)
            safety.submitReport("e1")
        }
        rule.waitForIdle()

        rule.onNodeWithText("Thanks, we'll review this.").assertIsDisplayed()
        assertNull(safety.state.value.snackbar)
    }

    @Test
    fun blockSuccessShowsYouBlockedName() {
        val api = FakeClickMeApi()
        api.initiateChatHandler = { initiateData("t9") }
        api.chatActionHandler = { _, _ -> StatusOnlyResponse("success") }
        val safety = safetyViewModel(api)
        setProfileScreen(api, safety)

        rule.runOnIdle { safety.confirmBlock("e1", "Ada") }
        rule.waitForIdle()

        rule.onNodeWithText("You blocked Ada").assertIsDisplayed()
        assertNull(safety.state.value.snackbar)
    }

    @Test
    fun blockSuccessWithoutNameShowsFallback() {
        val api = FakeClickMeApi()
        api.initiateChatHandler = { initiateData("t9") }
        api.chatActionHandler = { _, _ -> StatusOnlyResponse("success") }
        val safety = safetyViewModel(api)
        setProfileScreen(api, safety)

        rule.runOnIdle { safety.confirmBlock("e1", "  ") }
        rule.waitForIdle()

        rule.onNodeWithText("You blocked this user").assertIsDisplayed()
        assertNull(safety.state.value.snackbar)
    }

    private fun setHeartContent(
        isFavorite: Boolean,
        showFavorite: Boolean = true,
        onToggle: () -> Unit,
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertProfileContent(
                    state = loadedState(),
                    onBack = {},
                    onBookSession = {},
                    onRetry = {},
                    showFavorite = showFavorite,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggle,
                )
            }
        }
    }

    @Test
    fun heartShowsWhenAllowedAndCallsBack() {
        var taps = 0
        setHeartContent(isFavorite = false, onToggle = { taps++ })

        rule
            .onNodeWithContentDescription("Save expert")
            .assertHeightIsAtLeast(48.dp)
            .assertIsOff()
            .assertIsToggleable()
            .performClick()
        assertEquals(1, taps)
    }

    @Test
    fun favoritedHeartReadsRemoveFromSavedAndIsOn() {
        setHeartContent(isFavorite = true, onToggle = {})

        rule.onNodeWithContentDescription("Remove from saved").assertIsOn().assertIsToggleable()
        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
    }

    @Test
    fun heartIsAbsentWhenNotAllowed() {
        setHeartContent(isFavorite = false, showFavorite = false, onToggle = {})

        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Remove from saved").assertCountEquals(0)
    }

    @Test
    fun heartTapThroughTheRealViewModel() {
        val api = FakeClickMeApi()
        api.expertDetailsHandler = { PublicProfileDetailsData(isFavorite = false) }
        api.expertReviewsHandler = { ExpertReviewsPayload() }
        api.addFavoriteHandler = { toggleFavorite("e1", isFavorite = true) }
        val profile =
            ExpertProfileViewModel(
                ExpertProfileKey(expertId = "e1", name = "Ada", title = "", imageUrl = null),
                ExpertProfileRepository(api),
                FavoritesRepository(api),
            )
        val entry = testChatEntryViewModel(api)
        val safety = safetyViewModel(api)
        rule.setContent {
            ClickMeTheme {
                ExpertProfileScreen(
                    viewModel = profile,
                    onBack = {},
                    onBook = {},
                    chatEntry = entry,
                    safety = safety,
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Save expert").performClick()
        rule.waitForIdle()

        assertEquals(listOf("e1"), api.addFavoriteRequests)
        rule.onNodeWithContentDescription("Remove from saved").assertIsOn()
    }

    private class ProfileFixture(
        val api: FakeClickMeApi,
        val profile: ExpertProfileViewModel,
    )

    /** Builds every ViewModel outside setContent (lint ViewModelConstructorInComposable). */
    private fun setHeartScreen(
        isFavorite: Boolean = false,
        detailsGated: Boolean = false,
        isSelfView: Boolean = false,
        currentUserId: String? = "me",
        configure: (FakeClickMeApi) -> Unit = {},
    ): ProfileFixture {
        val api = FakeClickMeApi()
        api.expertDetailsHandler =
            if (detailsGated) {
                { CompletableDeferred<PublicProfileDetailsData>().await() }
            } else {
                { PublicProfileDetailsData(isFavorite = isFavorite) }
            }
        api.expertReviewsHandler = { ExpertReviewsPayload() }
        configure(api)
        val profile =
            ExpertProfileViewModel(
                ExpertProfileKey(expertId = "e1", name = "Ada", title = "", imageUrl = null),
                ExpertProfileRepository(api),
                FavoritesRepository(api),
            )
        val entry = testChatEntryViewModel(api, currentUserId = currentUserId)
        val safety = safetyViewModel(api)
        rule.setContent {
            ClickMeTheme {
                ExpertProfileScreen(
                    viewModel = profile,
                    onBack = {},
                    onBook = {},
                    isSelfView = isSelfView,
                    chatEntry = entry,
                    safety = safety,
                )
            }
        }
        rule.waitForIdle()
        return ProfileFixture(api, profile)
    }

    @Test
    fun heartFailureShowsSnackbarAndRollsBack() {
        val fixture =
            setHeartScreen { fake ->
                fake.addFavoriteHandler =
                    { _ ->
                        throw fake.httpError(
                            403,
                            """{"status":"error","code":"FORBIDDEN","message":"Client role required"}""",
                        )
                    }
            }

        rule.onNodeWithContentDescription("Save expert").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Client role required").assertIsDisplayed()
        rule.onNodeWithContentDescription("Save expert").assertIsOff()
        assertNull(fixture.profile.uiState.value.favoriteError)
    }

    @Test
    fun heartHiddenForSelfView() {
        setHeartScreen(isSelfView = true)

        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Remove from saved").assertCountEquals(0)
    }

    @Test
    fun heartHiddenForOwnProfile() {
        setHeartScreen(currentUserId = "E1")

        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Remove from saved").assertCountEquals(0)
    }

    @Test
    fun heartHiddenBeforeDetailsLoad() {
        setHeartScreen(detailsGated = true)

        rule.onAllNodesWithContentDescription("Save expert").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Remove from saved").assertCountEquals(0)
    }

    @Test
    fun heartShowsForAnotherExpertOnceDetailsLoad() {
        setHeartScreen(isFavorite = true)

        rule.onNodeWithContentDescription("Remove from saved").assertIsOn()
    }
}
