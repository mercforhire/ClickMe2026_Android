package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.BOOK_SESSION_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileAboutCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileActionButtons
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileExpertiseSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileHero
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileStatsBar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
}
