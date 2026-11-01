package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews.FAVORITE_EXPERT_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews.FAVORITE_PHOTO_FALLBACK_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.favoriteItem
import com.uptrendinvestments.clickmeapp.testing.favoritesPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class FavoritesScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val launchRequest = BookingsLaunchRequest()

    /** Holds the list response until the first composition has finished asking for it. */
    private val gate = CompletableDeferred<Unit>()

    private val opened = mutableListOf<ExpertProfileKey>()
    private var backs = 0
    private var explores = 0
    private var histories = 0

    private val ada = favoriteItem(id = "e1", name = "Ada Lovelace", headline = "Engineer", tags = listOf("AI"))
    private val grace =
        favoriteItem(id = "e2", name = "Grace Hopper", headline = "Admiral", tags = listOf("Navy", "COBOL"))

    private fun stub(vararg rows: com.uptrendinvestments.clickmeapp.model.responses.FavoriteExpertItem) {
        api.getClientFavoritesHandler = { _, _ ->
            // A real request suspends, which is what lets the screen's two first-show calls coalesce.
            gate.await()
            favoritesPayload(*rows)
        }
    }

    /** Built outside `setContent` (lint ViewModelConstructorInComposable). */
    private fun viewModel() = FavoritesViewModel(FavoritesRepository(api), launchRequest)

    private fun showScreen(viewModel: FavoritesViewModel = viewModel()) {
        rule.setContent {
            ClickMeTheme {
                FavoritesScreen(
                    viewModel = viewModel,
                    onBack = { backs++ },
                    onOpenExpert = { opened += it },
                    onExplore = { explores++ },
                    onViewHistory = { histories++ },
                )
            }
        }
        rule.waitForIdle()
        gate.complete(Unit)
        rule.waitForIdle()
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    // ---- Task 1: populated list ---------------------------------------------------------------------------------

    @Test
    fun populatedListShowsTaglineAndCardsWithoutProTip() {
        stub(ada, grace)
        showScreen()

        rule.onNodeWithText("Favorite Experts").assertIsDisplayed()
        rule.assertIsHeading("Favorite Experts")
        rule.onNodeWithText("Manage your curated list of elite professionals.").assertIsDisplayed()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Grace Hopper").assertExists()
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("AI").assertExists()
        rule.onNodeWithText("COBOL").assertExists()
        assertTrue(!exists("Pro Tip:"))
        assertEquals(1, api.getClientFavoritesCalls)
    }

    @Test
    fun cardTapOpensTheProfileOnTheProfileStack() {
        stub(ada, grace)
        showScreen()

        rule.onNodeWithText("Ada Lovelace").performClick()

        assertEquals(
            listOf(
                ExpertProfileKey("e1", "Ada Lovelace", "Engineer", null, null, null, listOf("AI"), StackOrigin.Profile),
            ),
            opened,
        )
    }

    @Test
    fun nullFieldsUseFallbacks() {
        stub(favoriteItem(id = "e3", name = null, headline = null, tags = null))
        showScreen()

        rule.onNodeWithText("Expert").assertExists()
        rule.onNodeWithTag(FAVORITE_PHOTO_FALLBACK_TAG, useUnmergedTree = true).assertIsDisplayed()
        // No headline text and no tag chips: the only texts under the card are the name.
        assertTrue(!exists("Engineer"))
        assertEquals(
            1,
            rule
                .onAllNodesWithText("Expert", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun cardHasTwoTalkBackStops() {
        stub(ada)
        showScreen()

        val card = rule.onNodeWithTag(FAVORITE_EXPERT_CARD_TAG).fetchSemanticsNode()
        val description =
            card.config
                .getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
                .joinToString(" ")
        assertTrue(description, description.contains("Ada Lovelace"))
        assertTrue(description, description.contains("Engineer"))
        assertTrue(description, description.contains("Expertise: AI"))
        val click = card.config.getOrNull(SemanticsActions.OnClick)
        assertNotNull(click)
        assertEquals("Open profile", click?.label)

        val heart = rule.onNodeWithContentDescription("Remove Ada Lovelace from favorites")
        heart.assertIsDisplayed()
        val bounds = heart.getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        assertTrue("heart $width x $height", width >= 48.dp && height >= 48.dp)
    }

    @Test
    fun backArrowCallsOnBack() {
        stub(ada)
        showScreen()

        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
