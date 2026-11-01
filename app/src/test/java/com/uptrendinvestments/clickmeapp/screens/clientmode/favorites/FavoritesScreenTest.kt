package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews.FAVORITE_EXPERT_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews.FAVORITE_PHOTO_FALLBACK_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.favoriteItem
import com.uptrendinvestments.clickmeapp.testing.favoritesPayload
import com.uptrendinvestments.clickmeapp.testing.toggleFavorite
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
import org.robolectric.shadows.ShadowDialog
import java.io.IOException

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

    private var retries = 0
    private var refreshes = 0
    private var refreshFailureConsumed = 0
    private val removed = mutableListOf<String>()
    private val openedRows = mutableListOf<FavoriteExpertUi>()

    private fun showContent(state: FavoritesUiState) {
        rule.setContent {
            ClickMeTheme {
                FavoritesContent(
                    state = state,
                    onBack = { backs++ },
                    onOpenExpert = { openedRows += it },
                    onRemoveConfirmed = { removed += it },
                    onExplore = { explores++ },
                    onViewHistory = { histories++ },
                    onRetry = { retries++ },
                    onRefresh = { refreshes++ },
                    onRefreshFailureConsumed = { refreshFailureConsumed++ },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun loaded(vararg rows: FavoriteExpertUi) = FavoritesUiState(load = FavoritesLoad.Loaded(rows.toList()))

    private fun top(text: String) = rule.onNodeWithText(text).getUnclippedBoundsInRoot().top

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

    // ---- Task 2: empty state, skeleton, error, refresh ----------------------------------------------------------

    @Test
    fun emptyStateShowsEverythingInOrder() {
        showContent(loaded())

        rule.assertIsHeading("No favorites yet")
        // The Pro Tip label and body are one merged node, so they share a top.
        val tops =
            listOf(
                top("No favorites yet"),
                top("Start exploring and save your favorite experts to find them easily later."),
                top("Explore Experts"),
                top("View My History"),
                top("Pro Tip:"),
            )
        assertEquals(tops.sortedBy { it.value }, tops)
        assertEquals(tops.size, tops.map { it.value }.distinct().size)
        assertEquals(
            top("Pro Tip:"),
            top("Tap the heart icon on any expert's profile to add them to this list instantly."),
        )
        assertTrue(!exists("Manage your curated list of elite professionals."))
    }

    @Test
    fun exploreButtonCallsOnExplore() {
        showContent(loaded())

        rule.onNodeWithText("Explore Experts").performClick()

        assertEquals(1, explores)
        assertEquals(0, histories)
    }

    @Test
    fun historyButtonCallsOnViewHistory() {
        showContent(loaded())

        rule.onNodeWithText("View My History").performClick()

        assertEquals(1, histories)
        assertEquals(0, explores)
    }

    @Test
    fun historyThroughTheRealScreenPostsPastThenSwitches() {
        stub()
        showScreen()

        rule.onNodeWithText("View My History").performClick()

        assertEquals(BookingsLaunch(BookingsTab.Past), launchRequest.pending.value)
        assertEquals(1, histories)
    }

    @Test
    fun firstLoadShowsSkeletonCards() {
        showContent(FavoritesUiState())

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).assertCountEquals(3)
        assertTrue(!exists("Manage your curated list of elite professionals."))
    }

    @Test
    fun firstLoadErrorShowsErrorStateAndRetry() {
        showContent(FavoritesUiState(load = FavoritesLoad.Failed(FieldError.Text("Server down"))))

        rule.onNodeWithText("Couldn't load favorites").assertIsDisplayed()
        rule.onNodeWithText("Server down").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun refreshFailureKeepsRowsAndShowsTheSnackbar() {
        showContent(loaded(ada.toUi(), grace.toUi()).copy(refreshFailed = true))

        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Grace Hopper").assertExists()
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertIsDisplayed()
        rule.onNodeWithText("Retry").assertIsDisplayed()
        assertEquals(1, refreshFailureConsumed)
    }

    @Test
    fun buttonsAreTallEnough() {
        showContent(loaded())

        listOf("Explore Experts", "View My History").forEach { label ->
            val bounds = rule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label ${bounds.bottom - bounds.top}", bounds.bottom - bounds.top >= 56.dp)
        }
    }

    // ---- Task 3: remove dialog, optimistic removal, accessibility -----------------------------------------------

    private val dialogTitle = "Remove Ada Lovelace from favorites?"
    private val dialogBody = "You can favorite this expert again anytime from their profile."

    /** System back while a Compose dialog is showing: the dialog's own back dispatcher handles it. */
    @Suppress("DEPRECATION")
    private fun pressBackOnTheDialog() {
        val dialog = checkNotNull(ShadowDialog.getLatestDialog()) { "no dialog is showing" }
        rule.runOnUiThread { dialog.onBackPressed() }
    }

    private fun tapHeart(name: String) {
        rule.onNodeWithContentDescription("Remove $name from favorites").performClick()
        rule.waitForIdle()
    }

    @Test
    fun cardHeartOpensTheDialogNotTheProfile() {
        showContent(loaded(ada.toUi()))

        tapHeart("Ada Lovelace")

        rule.onNodeWithText(dialogTitle).assertIsDisplayed()
        rule.onNodeWithText(dialogBody).assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
        rule.onNodeWithText("Remove").assertIsDisplayed()
        assertTrue(openedRows.isEmpty())
        assertTrue(removed.isEmpty())
    }

    @Test
    fun dialogNameFallsBackToExpert() {
        showContent(loaded(favoriteItem(id = "e3", name = null).toUi()))

        tapHeart("Expert")

        rule.onNodeWithText("Remove Expert from favorites?").assertIsDisplayed()
    }

    @Test
    fun confirmCallsOnRemoveConfirmedAndClosesTheDialog() {
        showContent(loaded(ada.toUi()))
        tapHeart("Ada Lovelace")

        rule.onNodeWithText("Remove").performClick()
        rule.waitForIdle()

        assertEquals(listOf("e1"), removed)
        assertTrue(!exists(dialogTitle))
    }

    @Test
    fun cancelAndBackLeaveTheListUnchanged() {
        showContent(loaded(ada.toUi()))
        tapHeart("Ada Lovelace")

        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertTrue(!exists(dialogTitle))

        tapHeart("Ada Lovelace")
        assertTrue(exists(dialogTitle))
        pressBackOnTheDialog()
        rule.waitForIdle()

        assertTrue(!exists(dialogTitle))
        assertTrue(removed.isEmpty())
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun removeThroughTheRealViewModel() {
        stub(ada)
        api.removeFavoriteHandler = { toggleFavorite("e1", isFavorite = false) }
        showScreen()
        tapHeart("Ada Lovelace")

        rule.onNodeWithText("Remove").performClick()
        rule.waitForIdle()

        assertTrue(!exists(dialogTitle))
        assertEquals(listOf("e1"), api.removeFavoriteRequests)
        assertTrue(!exists("Ada Lovelace"))
        rule.onNodeWithText("No favorites yet").assertIsDisplayed()
    }

    @Test
    fun failedRemoveRestoresTheRowAndShowsTheSnackbar() {
        stub(ada, grace)
        api.removeFavoriteHandler = {
            throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"Expert not found."}""")
        }
        showScreen()
        tapHeart("Ada Lovelace")

        rule.onNodeWithText("Remove").performClick()
        rule.waitForIdle()

        assertEquals(listOf("e1"), api.removeFavoriteRequests)
        rule.onNodeWithText("Ada Lovelace").assertExists()
        assertTrue(top("Ada Lovelace") < top("Grace Hopper"))
        rule.onNodeWithText("Expert not found.").assertIsDisplayed()
    }

    @Test
    fun failedRemoveOfflineShowsTheGenericSnackbar() {
        stub(ada)
        api.removeFavoriteHandler = { throw IOException() }
        showScreen()
        tapHeart("Ada Lovelace")

        rule.onNodeWithText("Remove").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Couldn't update favorites").assertIsDisplayed()
    }

    @Test
    fun dialogTargetSurvivesRecreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                FavoritesContent(
                    state = loaded(ada.toUi()),
                    onBack = {},
                    onOpenExpert = {},
                    onRemoveConfirmed = {},
                    onExplore = {},
                    onViewHistory = {},
                    onRetry = {},
                    onRefresh = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        tapHeart("Ada Lovelace")
        assertTrue(exists(dialogTitle))

        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithText(dialogTitle).assertIsDisplayed()
    }

    @Test
    fun a11yAuditPassesOnTheListAndTheEmptyState() {
        showContent(loaded(ada.toUi(), grace.toUi()))
        rule.assertTouchTargets()
        rule.assertActionablesNamed()
    }

    @Test
    fun a11yAuditPassesOnTheEmptyState() {
        showContent(loaded())
        rule.assertTouchTargets()
        rule.assertActionablesNamed()
    }
}
