@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews.CLIENT_PROFILE_HISTORY_ROW_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.assertTraversalOrder
import com.uptrendinvestments.clickmeapp.testing.breakdown
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.expertClientProfile
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// A tall window composes every lazy item at once.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ExpertClientProfileScreenTest {
    private val main = MainDispatcherRule(UnconfinedTestDispatcher())
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(main).around(composeRule)

    private val rule get() = composeRule

    private val api = FakeClickMeApi()
    private var backCount = 0

    // Dates, amounts and "Client since" must not depend on the machine running the test.
    private val utc = ZoneId.of("UTC")
    private val netAmount get() = Money.formatMajor(63.75, "USD", Locale.US)

    private fun stub(profile: ExpertClientProfileData = expertClientProfile()) {
        api.getExpertClientHandler = { profile }
        api.expertBookingsHandler = { ExpertBookingsPayload(bookings = emptyList()) }
    }

    private fun vm() =
        ExpertClientProfileViewModel(
            ExpertClientProfileKey("c1", ExpertBookingOrigin.Bookings),
            ExpertClientProfileRepository(api),
            ExpertBookingsRepository(api),
        )

    private fun show(viewModel: ExpertClientProfileViewModel = vm()) {
        rule.setContent {
            ClickMeTheme { ExpertClientProfileScreen(viewModel = viewModel, onBack = { backCount++ }) }
        }
    }

    private fun at(date: String): Instant = Instant.parse("${date}T15:00:00Z")

    private fun row(
        id: String = "b1",
        status: String = "completed",
        topic: String? = "Algorithms",
        start: Instant = at("2026-10-01"),
        free: Boolean = false,
    ): ClientHistoryRow {
        val item: ExpertBookingItem =
            expertBookingItem(
                id = id,
                statusCode = status,
                topic = topic,
                start = start,
                breakdown = if (free) null else breakdown(),
            )
        return ClientHistoryRow(item, clientHistoryBucket(status))
    }

    private fun showLoaded(
        profile: ExpertClientProfileData = expertClientProfile(),
        history: List<ClientHistoryRow> = emptyList(),
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ClickMeTheme {
                    ExpertClientProfileContent(
                        state = ExpertClientProfileUiState(load = ClientProfileLoad.Loaded(profile, history)),
                        onBack = {},
                        onRetry = {},
                        onRefresh = {},
                        onRefreshFailureConsumed = {},
                        zone = utc,
                        locale = Locale.US,
                    )
                }
            }
        }
    }

    // Cards and rows merge into one TalkBack node, so visible text is found in the unmerged tree.
    private fun shownCount(text: String) =
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun shown(text: String) = shownCount(text) > 0

    @Test
    fun aLoadedProfileShowsTheBarNameAndBadge() {
        stub(expertClientProfile(fullName = "Sophia Carter"))
        show()
        rule.onNodeWithText("Client Profile").assertExists()
        assertEquals(1, shownCount("Sophia Carter"))
        assertEquals(1, shownCount("Client"))
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun aNullNameFallsBackToClient() {
        stub(expertClientProfile(fullName = null))
        show()
        // The fallback name and the badge both read "Client".
        assertEquals(2, shownCount("Client"))
    }

    @Test
    fun anAvatarUrlDoesNotBlockTheScreen() {
        stub(expertClientProfile(fullName = "Sophia Carter"))
        show()
        assertEquals(1, shownCount("Sophia Carter"))
        assertEquals(1, shownCount("Client"))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun aboutShowsEveryRow() {
        showLoaded(expertClientProfile(bio = "Loves learning."))
        assertTrue(shown("About"))
        assertTrue(shown("Bio"))
        assertTrue(shown("Loves learning."))
        assertTrue(shown("Location"))
        assertTrue(shown("Toronto, ON, CAN"))
        assertTrue(shown("Timezone"))
        assertTrue(shown("(GMT-05:00) Eastern Time"))
        assertTrue(shown("Languages"))
        assertTrue(shown("English, Spanish"))
        assertTrue(shown("Member since"))
        assertTrue(shown("March 2025"))
    }

    @Test
    fun anEmptyAboutRowIsHidden() {
        showLoaded(
            expertClientProfile(
                bio = null,
                city = null,
                stateProvince = null,
                country = null,
                timezone = "(GMT-05:00) Eastern Time",
                languages = emptyList(),
                memberSince = null,
            ),
        )
        assertTrue(shown("About"))
        assertFalse(shown("Bio"))
        assertFalse(shown("Location"))
        assertFalse(shown("Languages"))
        assertFalse(shown("Member since"))
        assertTrue(shown("Timezone"))
    }

    @Test
    fun aBlankBioAndBlankLocationPartsCountAsEmpty() {
        showLoaded(expertClientProfile(bio = "   ", city = "", stateProvince = "  ", country = " ", timezone = null))
        assertFalse(shown("Bio"))
        assertFalse(shown("Location"))
        assertFalse(shown("Timezone"))
        assertTrue(shown("Languages"))
    }

    @Test
    fun blankLocationPartsAreSkippedInTheJoin() {
        showLoaded(expertClientProfile(city = "Toronto", stateProvince = " ", country = "CAN"))
        assertTrue(shown("Toronto, CAN"))
    }

    @Test
    fun sessionsCardShowsCountAndMonths() {
        showLoaded(expertClientProfile(totalSessions = 3))
        assertTrue(shown("Your Sessions Together"))
        assertTrue(shown("3"))
        assertTrue(shown("Sessions"))
        assertTrue(shown("Feb 2026"))
        assertTrue(shown("Client since"))
        assertTrue(shown("Oct 2026"))
        assertTrue(shown("Last session"))
    }

    @Test
    fun oneSessionIsSingular() {
        showLoaded(expertClientProfile(totalSessions = 1))
        assertEquals(1, shownCount("Session"))
        assertFalse(shown("Sessions"))
    }

    @Test
    fun aNullDateShowsADash() {
        showLoaded(expertClientProfile(firstSessionDate = null, lastSessionDate = null))
        assertEquals(2, shownCount("—"))
    }

    @Test
    fun historyRowShowsTitleLabelDateAndAmount() {
        showLoaded(history = listOf(row()))
        assertTrue(shown("Booking History"))
        assertTrue(shown("Algorithms"))
        assertTrue(shown("Completed - Oct 1, 2026"))
        assertTrue(shown(netAmount))
    }

    @Test
    fun aFreeSessionShowsNoAmount() {
        showLoaded(history = listOf(row(free = true)))
        assertTrue(shown("Completed - Oct 1, 2026"))
        assertFalse(shown(netAmount))
    }

    @Test
    fun aCancelledRowShowsNoAmountAndItsLabel() {
        showLoaded(history = listOf(row(status = "cancelled")))
        assertTrue(shown("Cancelled - Oct 1, 2026"))
        assertFalse(shown(netAmount))
    }

    @Test
    fun anUpcomingRowShowsUpcoming() {
        showLoaded(history = listOf(row(status = "confirmed", start = at("2026-11-20"))))
        assertTrue(shown("Upcoming - Nov 20, 2026"))
        assertTrue(shown(netAmount))
    }

    @Test
    fun aMissingTopicReadsSession() {
        showLoaded(history = listOf(row(topic = null)))
        assertTrue(shown("Session"))
        rule.onNodeWithContentDescription("Session. Completed, Oct 1, 2026. Earnings $netAmount").assertExists()
    }

    @Test
    fun historyIsHiddenWhenEmpty() {
        showLoaded(history = emptyList())
        assertFalse(shown("Booking History"))
        assertTrue(shown("Your Sessions Together"))
    }

    @Test
    fun aHistoryRowIsOneNodeThatReadsTitleBucketDateAndEarnings() {
        showLoaded(history = listOf(row(), row(id = "b2", status = "cancelled", topic = "Pitch deck")))
        rule.onNodeWithContentDescription("Algorithms. Completed, Oct 1, 2026. Earnings $netAmount").assertExists()
        // No amount shows on a cancelled row, so the spoken text has no Earnings part.
        rule.onNodeWithContentDescription("Pitch deck. Cancelled, Oct 1, 2026").assertExists()
    }

    @Test
    fun rowsAreNotClickable() {
        showLoaded(history = listOf(row(), row(id = "b2", status = "confirmed", start = at("2026-11-20"))))
        val nodes =
            rule.onAllNodesWithTag(CLIENT_PROFILE_HISTORY_ROW_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(2, nodes.size)
        nodes.forEach { node ->
            assertFalse(SemanticsActions.OnClick in node.config)
            assertFalse(SemanticsProperties.Role in node.config)
        }
    }

    @Test
    fun sectionsAreInTheIosOrder() {
        showLoaded(history = listOf(row()))
        rule.assertTraversalOrder("Sophia Carter", "About", "Your Sessions Together", "Booking History")
        rule.assertIsHeading("Sophia Carter")
        rule.assertIsHeading("About")
        rule.assertIsHeading("Your Sessions Together")
        rule.assertIsHeading("Booking History")
    }

    @Test
    fun everyHistoryRowShows() {
        showLoaded(history = (1..4).map { row(id = "b$it", topic = "Topic $it") })
        (1..4).forEach { assertTrue(shown("Topic $it")) }
    }

    private fun showState(
        state: ExpertClientProfileUiState,
        onRetry: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onRefreshFailureConsumed: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertClientProfileContent(
                    state = state,
                    onBack = { backCount++ },
                    onRetry = onRetry,
                    onRefresh = onRefresh,
                    onRefreshFailureConsumed = onRefreshFailureConsumed,
                    zone = utc,
                    locale = Locale.US,
                )
            }
        }
    }

    @Test
    fun loadingShowsTheSkeleton() {
        showState(ExpertClientProfileUiState(load = ClientProfileLoad.Loading))
        assertEquals(1, rule.onAllNodesWithTag(CLIENT_PROFILE_SKELETON_TAG).fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodesWithTag(CLIENT_PROFILE_LIST_TAG).fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithContentDescription("Loading").fetchSemanticsNodes().size)
    }

    @Test
    fun failedShowsTheErrorWithRetry() {
        var retries = 0
        showState(
            ExpertClientProfileUiState(load = ClientProfileLoad.Failed(FieldError.Text("Boom"))),
            onRetry = { retries++ },
        )
        assertTrue(shown("Couldn't load profile"))
        assertTrue(shown("Boom"))
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun noRelationshipShowsTheFriendlyStateWithNoRetry() {
        showState(ExpertClientProfileUiState(load = ClientProfileLoad.NoRelationship))
        assertTrue(shown("No bookings with this person yet"))
        assertTrue(shown("You can see someone's profile once they have booked a session with you."))
        assertFalse(shown("Retry"))
        rule.onNodeWithText("Go back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun unavailableShowsItsStateWithNoRetry() {
        showState(ExpertClientProfileUiState(load = ClientProfileLoad.Unavailable))
        assertTrue(shown("Profile unavailable"))
        assertTrue(shown("This person may no longer be on ClickMe."))
        assertFalse(shown("Retry"))
        rule.onNodeWithText("Go back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun theRealViewModel403ReachesTheFriendlyState() {
        stub()
        api.getExpertClientHandler = {
            throw api.httpError(403, """{"status":"error","code":"NO_RELATIONSHIP","message":"No relationship"}""")
        }
        show()
        assertTrue(shown("No bookings with this person yet"))
        assertFalse(shown("Retry"))
        assertEquals(0, api.expertBookingsCalls)
    }

    @Test
    fun refreshFailureShowsTheSnackbarAndKeepsContent() {
        var consumed = 0
        var refreshes = 0
        showState(
            ExpertClientProfileUiState(
                load = ClientProfileLoad.Loaded(expertClientProfile(fullName = "Sophia Carter"), emptyList()),
                refreshFailed = true,
            ),
            onRefresh = { refreshes++ },
            onRefreshFailureConsumed = { consumed++ },
        )
        rule.waitForIdle()
        assertTrue(shown("Sophia Carter"))
        assertTrue(shown("Couldn't refresh. Check your connection and try again."))
        assertEquals(1, consumed)
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, refreshes)
    }

    private fun assertNamedAndLargeEnough(load: ClientProfileLoad) {
        showState(ExpertClientProfileUiState(load = load))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun loadedIsNamedAndLargeEnough() =
        assertNamedAndLargeEnough(ClientProfileLoad.Loaded(expertClientProfile(), listOf(row())))

    @Test
    fun failedIsNamedAndLargeEnough() = assertNamedAndLargeEnough(ClientProfileLoad.Failed(FieldError.Text("Boom")))

    @Test
    fun noRelationshipIsNamedAndLargeEnough() = assertNamedAndLargeEnough(ClientProfileLoad.NoRelationship)

    @Test
    fun unavailableIsNamedAndLargeEnough() = assertNamedAndLargeEnough(ClientProfileLoad.Unavailable)

    @Test
    @Config(qualifiers = "w320dp-h3000dp")
    fun largeFontDoesNotClip() {
        val longTopic = "A very long consultation topic that keeps going and going for several lines of text"
        showLoaded(
            profile = expertClientProfile(bio = "word ".repeat(40).trim()),
            history = listOf(row(topic = longTopic)),
            fontScale = 2f,
        )
        rule.onNodeWithText("Client since", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Last session", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Toronto, ON, CAN", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("English, Spanish", useUnmergedTree = true).assertIsDisplayed()
        val amount = rule.onNodeWithText(netAmount, useUnmergedTree = true)
        amount.assertIsDisplayed()
        assertTrue(amount.getUnclippedBoundsInRoot().right <= 320.dp)
        rule.onNodeWithText(longTopic, useUnmergedTree = true).assertIsDisplayed()
    }
}
