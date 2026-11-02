package com.uptrendinvestments.clickmeapp.screens

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.clientBookingNoteEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.EXPLORE_LIST_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.ExploreContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.ExploreUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.EXPLORE_EXPERTS_SEE_ALL_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.clientFavoritesEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.ClientHomeGate
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientShell
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientBookingFlowEntries
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientBookingManagementEntries
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientPaymentMethodsEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientProfileHubActions
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openBookingSummary
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openUpcomingBooking
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showSearch
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ClientProfileHomeContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ExpertReviewsKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.expertReviewsEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.reviewsKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchExpertContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchExpertUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchResults
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_EXPERT_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.toProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertShell
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile.TaxProfileKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorKey
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsKey
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsOrigin
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsKeyFor
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsRootFor
import com.uptrendinvestments.clickmeapp.screens.push.ShellRoute
import com.uptrendinvestments.clickmeapp.screens.push.ShellRouteRequest
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import com.uptrendinvestments.clickmeapp.testing.searchExpertUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class HomeShellsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val switched = mutableListOf<AppMode>()
    private var logoutCount = 0
    private var refreshCount = 0

    private fun setClient(
        showDebug: Boolean,
        refreshStatus: String? = null,
        paymentMethods: Boolean = false,
        hubStandIns: Boolean = false,
        versionLabel: String = "",
        chatLaunch: ChatLaunchRequest? = null,
        shellRoutes: ShellRouteRequest? = null,
    ) {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                val pendingThread = pendingThreadOf(chatLaunch)
                val pendingRoute = pendingRouteOf(shellRoutes)
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onSwitchMode = { switched += it },
                    onLogout = { logoutCount++ },
                    onDebugForceRefresh = { refreshCount++ },
                    debugRefreshStatus = refreshStatus,
                    profileRoot = { navigator, debug ->
                        ClientProfileHomeContent(
                            card = ProfileHubCard(null, null, null),
                            versionLabel = versionLabel,
                            actions = clientProfileHubActions(navigator, { switched += it }, { logoutCount++ }),
                            debugContent = debug,
                        )
                    },
                    detailEntries = { navigator ->
                        if (paymentMethods) fakePaymentMethodsEntry(navigator)
                        if (hubStandIns) clientHubStandIns(navigator)
                        fakeChatThreadEntry()
                        pushStandIns()
                    },
                    pendingChatThread = pendingThread,
                    onConsumeChatThread = { chatLaunch?.thread?.consume() },
                    pendingShellRoute = pendingRoute,
                    onConsumeShellRoute = { shellRoutes?.consumeFor(AppMode.Client) },
                )
            }
        }
    }

    private fun setExpert(
        showDebug: Boolean,
        refreshStatus: String? = null,
        hubStandIns: Boolean = false,
        chatLaunch: ChatLaunchRequest? = null,
        shellRoutes: ShellRouteRequest? = null,
    ) {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                val pendingThread = pendingThreadOf(chatLaunch)
                val pendingRoute = pendingRouteOf(shellRoutes)
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onSwitchMode = { switched += it },
                    onLogout = { logoutCount++ },
                    onDebugForceRefresh = { refreshCount++ },
                    debugRefreshStatus = refreshStatus,
                    detailEntries = { navigator ->
                        if (hubStandIns) expertHubStandIns(navigator)
                        fakeChatThreadEntry()
                        pushStandIns()
                    },
                    pendingChatThread = pendingThread,
                    onConsumeChatThread = { chatLaunch?.thread?.consume() },
                    pendingShellRoute = pendingRoute,
                    onConsumeShellRoute = { shellRoutes?.consumeFor(AppMode.Expert) },
                )
            }
        }
    }

    // Tab names also appear as TopAppBar titles and empty-state headings, so match the bar item.
    private fun tab(label: String) = hasText(label) and isSelectable()

    private fun topOf(
        text: String,
        substring: Boolean = false,
    ): Float {
        val node = composeRule.onNodeWithText(text, substring = substring).fetchSemanticsNode()
        return node.positionInRoot.y
    }

    private fun tapTab(label: String) = composeRule.onNode(tab(label)).performClick()

    private fun openGalleryFromProfile() {
        tapTab("Profile")
        // The Payment Methods row pushes the gallery button below the fold of the small test window.
        composeRule.onNodeWithText("Design system gallery").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun clientShellShowsFiveTabsWithExploreSelectedAndAvatarButton() {
        setClient(showDebug = true)
        listOf("Explore", "Search", "Bookings", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Explore")).assertIsSelected()
        composeRule.onNodeWithContentDescription("Your profile").assertExists()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun profileTabOffersGalleryAndNoLongerOffersScaffoldSwitch() {
        setClient(showDebug = true)
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertExists()
        composeRule.onNodeWithText("Switch to expert scaffold").assertDoesNotExist()
    }

    @Test
    fun clientAvatarOpensModeSwitcherWithoutBottomBarAndClientActive() {
        setClient(showDebug = false)
        composeRule.onNodeWithContentDescription("Your profile").performClick()
        composeRule.onNodeWithText("Mode Switcher").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        composeRule.onNode(hasText("Client Mode") and hasText("Active")).assertExists()
        composeRule.onNode(hasText("Expert Mode") and hasText("Active")).assertDoesNotExist()
    }

    @Test
    fun clientChoosingExpertModeInvokesCallbackOnceAndReturnsToExploreWithBar() {
        setClient(showDebug = false)
        composeRule.onNodeWithContentDescription("Your profile").performClick()
        composeRule.onNode(hasText("Expert Mode")).performClick()
        assertEquals(listOf(AppMode.Expert), switched)
        composeRule.onNodeWithText("Mode Switcher").assertDoesNotExist()
        composeRule.onNode(tab("Explore")).assertIsSelected()
    }

    @Test
    fun expertAvatarOpensModeSwitcherWithExpertActive() {
        setExpert(showDebug = false)
        composeRule.onNodeWithContentDescription("Your profile").performClick()
        composeRule.onNodeWithText("Mode Switcher").assertExists()
        composeRule.onNode(tab("Dashboard")).assertDoesNotExist()
        composeRule.onNode(hasText("Expert Mode") and hasText("Active")).assertExists()
    }

    @Test
    fun expertChoosingClientModeInvokesCallback() {
        setExpert(showDebug = false)
        composeRule.onNodeWithContentDescription("Your profile").performClick()
        composeRule.onNode(hasText("Client Mode")).performClick()
        assertEquals(listOf(AppMode.Client), switched)
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
    }

    @Test
    fun gateShowsCarouselWithoutHomeWhenOnboardingIsPending() {
        composeRule.setContent {
            ClickMeTheme {
                ClientHomeGate(
                    showOnboarding = true,
                    onboarding = { Text("Welcome to ClickMe") },
                    home = { Text("HOME") },
                )
            }
        }
        composeRule.onNodeWithText("Welcome to ClickMe").assertExists()
        composeRule.onNodeWithText("HOME").assertDoesNotExist()
    }

    @Test
    fun gateShowsHomeWithoutCarouselOnceSeen() {
        composeRule.setContent {
            ClickMeTheme {
                ClientHomeGate(
                    showOnboarding = false,
                    onboarding = { Text("Welcome to ClickMe") },
                    home = { Text("HOME") },
                )
            }
        }
        composeRule.onNodeWithText("HOME").assertExists()
        composeRule.onNodeWithText("Welcome to ClickMe").assertDoesNotExist()
    }

    @Test
    fun galleryShowsBackAndAllSections() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        listOf("Typography", "AsyncSaveButton", "Avatar").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertExists()
        }
    }

    @Test
    fun galleryStaysOnProfileStackAcrossTabSwitch() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        tapTab("Explore")
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        tapTab("Profile")
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun retappingSelectedProfileTabPopsToRoot() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        tapTab("Profile")
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Design system gallery").assertExists()
    }

    @Test
    fun backNodeReturnsToProfileRoot() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Design system gallery").assertExists()
    }

    @Test
    fun debugAffordancesAbsentWhenGateIsOff() {
        setClient(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertDoesNotExist()
    }

    @Test
    fun expertShellShowsFiveTabsWithDashboardSelectedAndAvatarButton() {
        setExpert(showDebug = true)
        listOf("Dashboard", "Bookings", "Topics", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
        composeRule.onNodeWithContentDescription("Your profile").assertExists()
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertExists()
    }

    @Test
    fun expertDebugAffordancesAbsentWhenGateIsOff() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertDoesNotExist()
    }

    @Test
    fun clientProfileTabIsTheHub() {
        setClient(showDebug = false)
        tapTab("Profile")
        listOf(
            "Discovery",
            "Saved Experts",
            "Security & Password",
            "Payment",
            "FAQ & Help",
            "Privacy",
            "Log out",
        ).forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithText("Payment Methods").assertDoesNotExist()
        composeRule.onNodeWithText("Force token refresh").assertDoesNotExist()
    }

    @Test
    fun clientHubRowsNavigate() {
        setClient(showDebug = false, paymentMethods = true, hubStandIns = true)
        listOf(
            "Saved Experts" to "stand-in favorites",
            "Security & Password" to "stand-in security",
            "FAQ & Help" to "stand-in help",
            "Payment" to "fake payment methods",
        ).forEach { (row, screen) ->
            tapTab("Profile")
            composeRule.onNodeWithText(row).performScrollTo().performClick()
            composeRule.onNodeWithText(screen).assertExists()
            composeRule.onNode(tab("Profile")).assertDoesNotExist()
            pressSystemBack()
            composeRule.onNode(tab("Profile")).assertIsSelected()
            composeRule.onNodeWithText(screen).assertDoesNotExist()
        }
        composeRule.onNodeWithContentDescription("Edit profile", substring = true).performScrollTo().performClick()
        composeRule.onNodeWithText("stand-in client settings").assertExists()
        composeRule.onNode(tab("Profile")).assertDoesNotExist()
    }

    @Test
    fun savedExpertsExploreButtonSelectsTheExploreRoot() {
        setClient(showDebug = false, hubStandIns = true)
        tapTab("Profile")
        composeRule.onNodeWithText("Saved Experts").performScrollTo().performClick()
        composeRule.onNodeWithText("stand-in favorites").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()

        composeRule.onNodeWithText("stand-in explore experts").performClick()

        composeRule.onNode(tab("Explore")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
        composeRule.onNodeWithText("stand-in favorites").assertDoesNotExist()
    }

    @Test
    fun savedExpertsHistoryButtonSelectsBookings() {
        setClient(showDebug = false, hubStandIns = true)
        tapTab("Profile")
        composeRule.onNodeWithText("Saved Experts").performScrollTo().performClick()

        composeRule.onNodeWithText("stand-in view history").performClick()

        composeRule.onNode(tab("Bookings")).assertIsSelected()
        composeRule.onNodeWithText("stand-in favorites").assertDoesNotExist()
        // RESEARCH A3: the Profile stack is not reset, so Saved Experts is still open there.
        tapTab("Profile")
        composeRule.onNodeWithText("stand-in favorites").assertExists()
    }

    @Test
    fun clientLogOutGoesThroughDialog() {
        setClient(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Log out").performScrollTo().performClick()
        composeRule.onNodeWithText("Stay logged in").performClick()
        assertEquals(0, logoutCount)
        composeRule.onNodeWithText("Log out").performScrollTo().performClick()
        composeRule.onNodeWithText("Log Out").performClick()
        assertEquals(1, logoutCount)
    }

    @Test
    fun clientModeCardSwitches() {
        setClient(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Switch to Expert Mode").performClick()
        assertEquals(listOf(AppMode.Expert), switched)
    }

    @Test
    fun clientDebugAffordancesBelowFooter() {
        setClient(showDebug = true, refreshStatus = "Token refresh OK", versionLabel = "Version 9.9 (9)")
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").performScrollTo().assertExists()
        // Measured after the scroll so both tops share one scroll offset.
        assertTrue(topOf("Version", substring = true) < topOf("Design system gallery"))
        composeRule.onNodeWithText("Token refresh OK").assertExists()
        composeRule.onNodeWithText("Force token refresh").performScrollTo().performClick()
        assertEquals(1, refreshCount)
    }

    @Test
    fun expertProfileTabIsTheHub() {
        setExpert(showDebug = false)
        tapTab("Profile")
        listOf(
            "Professional",
            "Account",
            "Support",
            "View My Public Profile",
            "Expertise & Topics",
            "Availability",
            "Payouts & Earnings",
            "Tax & Payout Info",
            "Security & Password",
            "FAQ & Help",
            "Log out",
        ).forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Payment").assertDoesNotExist()
        composeRule.onNodeWithText("Force token refresh").assertDoesNotExist()
    }

    @Test
    fun expertHubRowsNavigate() {
        setExpert(showDebug = false, hubStandIns = true)
        listOf(
            "Payouts & Earnings" to "stand-in payouts:Profile",
            "Tax & Payout Info" to "stand-in tax:Profile",
            "Security & Password" to "stand-in security",
            "FAQ & Help" to "stand-in help",
        ).forEach { (row, screen) ->
            tapTab("Profile")
            composeRule.onNodeWithText(row).performScrollTo().performClick()
            composeRule.onNodeWithText(screen).assertExists()
            composeRule.onNode(tab("Profile")).assertDoesNotExist()
            pressSystemBack()
            composeRule.onNode(tab("Profile")).assertIsSelected()
            composeRule.onNodeWithText(screen).assertDoesNotExist()
        }
        composeRule.onNodeWithContentDescription("Edit profile", substring = true).performScrollTo().performClick()
        composeRule.onNodeWithText("stand-in expert settings:Profile").assertExists()
        composeRule.onNode(tab("Profile")).assertDoesNotExist()
    }

    @Test
    fun expertiseAndTopicsSelectsTopicsTab() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Expertise & Topics").performScrollTo().performClick()
        composeRule.onNode(tab("Topics")).assertIsSelected()
    }

    @Test
    fun expertLogOutGoesThroughDialog() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Log out").performScrollTo().performClick()
        composeRule.onNodeWithText("Stay logged in").performClick()
        assertEquals(0, logoutCount)
        composeRule.onNodeWithText("Log out").performScrollTo().performClick()
        composeRule.onNodeWithText("Log Out").performClick()
        assertEquals(1, logoutCount)
    }

    @Test
    fun expertModeCardSwitches() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Switch to Client Mode").performClick()
        assertEquals(listOf(AppMode.Client), switched)
    }

    @Test
    fun expertDebugProfileOffersForceRefreshAndShowsStatus() {
        setExpert(showDebug = true, refreshStatus = "Refresh failed: boom")
        tapTab("Profile")
        composeRule.onNodeWithText("Log out").assertExists()
        composeRule.onNodeWithText("Refresh failed: boom").assertExists()
        composeRule.onNodeWithText("Force token refresh").performScrollTo().performClick()
        assertEquals(1, refreshCount)
    }

    private fun setClientWithExploreButton(
        label: String,
        target: (Navigator) -> Unit,
        detail: EntryProviderScope<NavKey>.(Navigator) -> Unit = {},
    ) {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = "https://example.com/me.png",
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, url ->
                        Button(onClick = { target(navigator) }) { Text("$label $url") }
                    },
                    detailEntries = detail,
                )
            }
        }
    }

    /** Fake Payment Methods content registered through the real [clientPaymentMethodsEntry]. */
    private fun EntryProviderScope<NavKey>.fakePaymentMethodsEntry(navigator: Navigator) {
        clientPaymentMethodsEntry(navigator) { onBack ->
            Column {
                Text("fake payment methods")
                Button(onClick = onBack) { Text("pm back") }
            }
        }
    }

    /** Stand-ins for the client hub destinations, registered with the same hidden-bar metadata as the real ones. */
    private fun EntryProviderScope<NavKey>.clientHubStandIns(navigator: Navigator) {
        clientFavoritesEntry(navigator) { actions ->
            Column {
                Text("stand-in favorites")
                Button(onClick = actions.onExplore) { Text("stand-in explore experts") }
                Button(onClick = actions.onViewHistory) { Text("stand-in view history") }
            }
        }
        entry<ClientProfileSettingsKey>(metadata = BottomBar.hidden()) { Text("stand-in client settings") }
        entry<AccountSecurityKey>(metadata = BottomBar.hidden()) { Text("stand-in security") }
        entry<HelpKey>(metadata = BottomBar.hidden()) { Text("stand-in help") }
    }

    /** Stand-ins for the expert hub destinations, registered with the same hidden-bar metadata as the real ones. */
    private fun EntryProviderScope<NavKey>.expertHubStandIns(navigator: Navigator) {
        entry<ExpertProfileSettingsKey>(metadata = BottomBar.hidden()) { Text("stand-in expert settings:${it.origin}") }
        entry<PayoutsKey>(metadata = BottomBar.hidden()) { Text("stand-in payouts:${it.origin}") }
        entry<TaxProfileKey>(metadata = BottomBar.hidden()) { Text("stand-in tax:${it.origin}") }
        entry<AccountSecurityKey>(metadata = BottomBar.hidden()) { Text("stand-in security") }
        entry<HelpKey>(metadata = BottomBar.hidden()) { Text("stand-in help") }
        fakeAvailabilityEntry(navigator)
    }

    private var bookingEntered = 0
    private var bookingResumed = 0

    private fun setClientWithBookingFlow() {
        setClientWithExploreButton(
            label = "open booking",
            target = { it.navigate(BookingKey("e1", "Ada Lovelace", "Engineer", null)) },
            detail = { nav ->
                clientBookingFlowEntries(
                    navigator = nav,
                    booking = { _, onBack, onBooked, onOpenPaymentMethods ->
                        LaunchedEffect(Unit) { bookingEntered++ }
                        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { bookingResumed++ }
                        Column {
                            Button(onClick = onBack) { Text("fake back") }
                            Button(onClick = { onBooked("b1") }) { Text("fake book") }
                            Button(onClick = onOpenPaymentMethods) { Text("fake add card") }
                        }
                    },
                    requestSent = { key, onBack, onViewBookings, onBackToHome ->
                        Column {
                            Text("sent ${key.bookingId}")
                            Button(onClick = onBack) { Text("sent back") }
                            Button(onClick = onViewBookings) { Text("sent bookings") }
                            Button(onClick = onBackToHome) { Text("sent home") }
                        }
                    },
                )
                fakePaymentMethodsEntry(nav)
            },
        )
    }

    private fun openRequestSent() {
        setClientWithBookingFlow()
        composeRule.onNodeWithText("open booking", substring = true).performClick()
        composeRule.onNodeWithText("fake book").performClick()
        composeRule.onNodeWithText("sent b1").assertExists()
    }

    @Test
    fun bookingRouteHidesBottomBarAndBackRestoresIt() {
        setClientWithBookingFlow()
        composeRule.onNodeWithText("open booking", substring = true).performClick()
        composeRule.onNodeWithText("fake back").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        composeRule.onNodeWithText("fake back").performClick()
        composeRule.onNodeWithText("open booking", substring = true).assertExists()
        composeRule.onNode(tab("Explore")).assertExists()
    }

    @Test
    fun bookingSuccessReplacesBookingWithRequestSent() {
        openRequestSent()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        composeRule.onNodeWithText("sent back").performClick()
        composeRule.onNodeWithText("open booking", substring = true).assertExists()
        composeRule.onNodeWithText("fake back").assertDoesNotExist()
    }

    @Test
    fun viewMyBookingsResetsExploreAndSelectsBookings() {
        openRequestSent()
        composeRule.onNodeWithText("sent bookings").performClick()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        tapTab("Explore")
        composeRule.onNodeWithText("open booking", substring = true).assertExists()
        composeRule.onNodeWithText("sent b1").assertDoesNotExist()
    }

    @Test
    fun exploreSeeAllSelectsTheSearchTab() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, url ->
                        ExploreContent(
                            state = ExploreUiState(),
                            avatarUrl = url,
                            onAvatarClick = {},
                            onRefresh = {},
                            onRetry = {},
                            onChipTap = {},
                            onViewAll = {},
                            onClearFilter = {},
                            onExpertClick = {},
                            onFeaturedTopicClick = {},
                            onSessionClick = {},
                            joinNow = Instant.EPOCH,
                            onJoinSession = {},
                            onRefreshFailureConsumed = {},
                            onSeeAll = navigator::showSearch,
                        )
                    },
                )
            }
        }
        composeRule.onNodeWithTag(EXPLORE_LIST_TAG).performScrollToNode(hasTestTag(EXPLORE_EXPERTS_SEE_ALL_TAG))
        composeRule.onNodeWithTag(EXPLORE_EXPERTS_SEE_ALL_TAG).assertHasClickAction()
        composeRule.onNodeWithTag(EXPLORE_EXPERTS_SEE_ALL_TAG).assert(hasText("See all"))
        composeRule.onNodeWithTag(EXPLORE_EXPERTS_SEE_ALL_TAG).performClick()
        composeRule.onNode(tab("Search")).assertIsSelected()
        composeRule.onNode(tab("Explore")).assertIsNotSelected()
    }

    @Test
    fun searchTabRendersTheSearchSlotAndOpensAProfileOnTheSearchStack() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    searchRoot = { navigator ->
                        SearchExpertContent(
                            state = SearchExpertUiState(results = SearchResults.Loaded(listOf(searchExpertUi()))),
                            onQueryChange = {},
                            onCategorySelected = {},
                            onSortSelected = {},
                            onExpertClick = { navigator.navigate(it.toProfileKey("Expert")) },
                            onRefresh = {},
                            onRetry = {},
                            onRefreshFailureConsumed = {},
                        )
                    },
                    detailEntries = { entry<ExpertProfileKey> { Text("profile ${it.expertId} ${it.origin}") } },
                )
            }
        }
        tapTab("Search")
        composeRule.onNodeWithTag(SEARCH_EXPERT_CARD_TAG).performClick()
        composeRule.onNodeWithText("profile e1 Search").assertExists()
        composeRule.onNode(tab("Search")).assertIsSelected()
        tapTab("Explore")
        composeRule.onNodeWithText("profile e1 Search").assertDoesNotExist()
    }

    @Test
    fun defaultShellKeepsThePlaceholderOnTheSearchTab() {
        setClient(showDebug = false)
        tapTab("Search")
        composeRule.onNode(tab("Search")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun sameExpertOpenOnExploreAndSearchDoesNotCollide() {
        setClientWithSearchStandIn()
        composeRule.onNodeWithText("open explore profile").performClick()
        composeRule.onNodeWithText("profile e1 Default").assertExists()

        tapTab("Search")
        composeRule.onNodeWithText("open search profile").performClick()
        composeRule.onNodeWithText("profile e1 Search").assertExists()
        composeRule.onNode(tab("Search")).assertIsSelected()

        tapTab("Explore")
        composeRule.onNodeWithText("profile e1 Default").assertExists()
    }

    @Test
    fun searchStateSurvivesATabSwitch() {
        setClientWithSearchStandIn()
        tapTab("Search")
        composeRule.onNodeWithTag(SEARCH_STAND_IN_FIELD_TAG).performTextInput("ada")

        tapTab("Bookings")
        tapTab("Search")

        composeRule.onNodeWithTag(SEARCH_STAND_IN_FIELD_TAG).assertTextContains("ada")
    }

    @Test
    fun retappingSearchPopsToRootAndKeepsItsState() {
        setClientWithSearchStandIn()
        tapTab("Search")
        composeRule.onNodeWithTag(SEARCH_STAND_IN_FIELD_TAG).performTextInput("ada")
        composeRule.onNodeWithText("open search profile").performClick()
        composeRule.onNodeWithText("profile e1 Search").assertExists()

        tapTab("Search")

        composeRule.onNodeWithText("profile e1 Search").assertDoesNotExist()
        composeRule.onNodeWithTag(SEARCH_STAND_IN_FIELD_TAG).assertTextContains("ada")
        composeRule.onNode(tab("Search")).assertIsSelected()
    }

    /**
     * Explore and Search roots that each push the same expert, the Explore one with the Default origin and the Search one
     * through the real [toProfileKey]. The Search root keeps its text in a [SearchStandInViewModel] scoped to the Nav3
     * entry, the same scoping the Hilt SearchExpertScreen relies on (D-02).
     */
    private fun setClientWithSearchStandIn() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(
                            onClick = {
                                navigator.navigate(
                                    ExpertProfileKey("e1", "Ada Lovelace", "Engineer", null, 4.5, null, listOf("AI")),
                                )
                            },
                        ) { Text("open explore profile") }
                    },
                    searchRoot = { navigator ->
                        val holder: SearchStandInViewModel = viewModel { SearchStandInViewModel() }
                        Column {
                            OutlinedTextField(
                                value = holder.text,
                                onValueChange = { holder.text = it },
                                modifier = Modifier.testTag(SEARCH_STAND_IN_FIELD_TAG),
                            )
                            Button(onClick = { navigator.navigate(searchExpertUi().toProfileKey("Expert")) }) {
                                Text("open search profile")
                            }
                        }
                    },
                    detailEntries = { entry<ExpertProfileKey> { Text("profile ${it.expertId} ${it.origin}") } },
                )
            }
        }
    }

    /**
     * Explore root and the Saved Experts stand-in that each push the same expert with their own origin, the real
     * [clientFavoritesEntry] and [expertReviewsEntry], and a profile stand-in whose "see all" builds the Reviews key
     * through the real [reviewsKey]. The default Profile hub opens the stand-in favorites.
     */
    private fun setClientWithProfileAndReviewsStandIns() {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(ada()) }) { Text("open explore profile") }
                    },
                    detailEntries = { navigator ->
                        clientFavoritesEntry(navigator) { actions ->
                            Button(
                                onClick = { actions.onOpenExpert(ada().copy(origin = StackOrigin.Profile)) },
                            ) { Text("open favorite profile") }
                        }
                        entry<ExpertProfileKey> { key ->
                            Column {
                                Text("profile ${key.expertId} ${key.origin}")
                                Button(onClick = { navigator.navigate(key.reviewsKey(key.name)) }) {
                                    Text("see all ${key.origin}")
                                }
                            }
                        }
                        expertReviewsEntry(navigator) { key, onBack ->
                            Column {
                                Text("reviews ${key.expertId} ${key.origin}")
                                Button(onClick = onBack) { Text("reviews back") }
                            }
                        }
                    },
                )
            }
        }
    }

    /**
     * The booking flow started from Saved Experts (CR-01): the real hub row opens the real [clientFavoritesEntry],
     * whose button opens an expert on the Profile stack. That profile stand-in has no hidden-bar metadata, like the
     * production entry, and its "book" button opens the booking form. The real [clientBookingFlowEntries] supply the
     * Request Sent exits under test. The Explore root opens the same expert with the Default origin.
     */
    private fun setClientWithSavedExpertsBooking() {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(ada()) }) { Text("open explore profile") }
                    },
                    detailEntries = { navigator ->
                        clientFavoritesEntry(navigator) { actions ->
                            Button(
                                onClick = { actions.onOpenExpert(ada().copy(origin = StackOrigin.Profile)) },
                            ) { Text("open favorite profile") }
                        }
                        entry<ExpertProfileKey> { key ->
                            Column {
                                Text("profile ${key.expertId} ${key.origin}")
                                Button(
                                    onClick = {
                                        navigator.navigate(BookingKey("e1", "Ada Lovelace", "Engineer", null))
                                    },
                                ) { Text("book ${key.origin}") }
                            }
                        }
                        clientBookingFlowEntries(
                            navigator = navigator,
                            booking = { _, _, onBooked, _ ->
                                Button(onClick = { onBooked("b1") }) { Text("fake book") }
                            },
                            requestSent = { key, _, onViewBookings, onBackToHome ->
                                Column {
                                    Text("sent ${key.bookingId}")
                                    Button(onClick = onViewBookings) { Text("sent bookings") }
                                    Button(onClick = onBackToHome) { Text("sent home") }
                                }
                            },
                        )
                    },
                )
            }
        }
    }

    private fun ada() = ExpertProfileKey("e1", "Ada Lovelace", "Engineer", null, 4.5, null, listOf("AI"))

    private fun openFavoriteProfileOnTheProfileTab() {
        tapTab("Profile")
        composeRule.onNodeWithText("Saved Experts").performScrollTo().performClick()
        composeRule.onNodeWithText("open favorite profile").performClick()
    }

    private fun bookFromSavedExperts() {
        openFavoriteProfileOnTheProfileTab()
        composeRule.onNodeWithText("book Profile").performClick()
        composeRule.onNodeWithText("fake book").performClick()
        composeRule.onNodeWithText("sent b1").assertExists()
    }

    private fun assertProfileTabShowsTheHub() {
        tapTab("Profile")
        composeRule.onNode(tab("Profile")).assertIsSelected()
        composeRule.onNodeWithText("Saved Experts").assertExists()
        composeRule.onNodeWithText("sent b1").assertDoesNotExist()
        composeRule.onNodeWithText("open favorite profile").assertDoesNotExist()
        composeRule.onNodeWithText("profile e1 Profile").assertDoesNotExist()
    }

    @Test
    fun bookingFromSavedExpertsViewBookingsLeavesProfileOnTheHub() {
        setClientWithSavedExpertsBooking()
        bookFromSavedExperts()

        composeRule.onNodeWithText("sent bookings").performClick()

        composeRule.onNode(tab("Bookings")).assertIsSelected()
        assertProfileTabShowsTheHub()
    }

    @Test
    fun bookingFromSavedExpertsBackToHomeLeavesProfileOnTheHub() {
        setClientWithSavedExpertsBooking()
        bookFromSavedExperts()

        composeRule.onNodeWithText("sent home").performClick()

        composeRule.onNode(tab("Explore")).assertIsSelected()
        assertProfileTabShowsTheHub()
    }

    @Test
    fun bookingFromExploreLeavesASavedExpertProfileOpenOnTheProfileTab() {
        setClientWithSavedExpertsBooking()
        openFavoriteProfileOnTheProfileTab()
        tapTab("Explore")
        composeRule.onNodeWithText("open explore profile").performClick()
        composeRule.onNodeWithText("book Default").performClick()
        composeRule.onNodeWithText("fake book").performClick()
        composeRule.onNodeWithText("sent bookings").performClick()

        tapTab("Profile")

        composeRule.onNodeWithText("profile e1 Profile").assertExists()
        composeRule.onNodeWithText("sent b1").assertDoesNotExist()
    }

    @Test
    fun savedExpertsExploreButtonKeepsSavedExpertsOpenOnTheProfileTab() {
        // D-05, runbook row D1.FV4: Saved Experts' own button is not a finished booking, so the list stays open.
        setClient(showDebug = false, hubStandIns = true)
        tapTab("Profile")
        composeRule.onNodeWithText("Saved Experts").performScrollTo().performClick()

        composeRule.onNodeWithText("stand-in explore experts").performClick()
        tapTab("Profile")

        composeRule.onNodeWithText("stand-in favorites").assertExists()
    }

    @Test
    fun sameExpertOnExploreAndProfileDoesNotCollide() {
        setClientWithProfileAndReviewsStandIns()
        composeRule.onNodeWithText("open explore profile").performClick()
        composeRule.onNodeWithText("profile e1 Default").assertExists()

        openFavoriteProfileOnTheProfileTab()
        composeRule.onNodeWithText("profile e1 Profile").assertExists()
        composeRule.onNode(tab("Profile")).assertIsSelected()

        tapTab("Explore")
        composeRule.onNodeWithText("profile e1 Default").assertExists()
        tapTab("Profile")
        composeRule.onNodeWithText("profile e1 Profile").assertExists()
    }

    @Test
    fun seeAllOnTwoStacksDoesNotCollide() {
        setClientWithProfileAndReviewsStandIns()
        composeRule.onNodeWithText("open explore profile").performClick()
        composeRule.onNodeWithText("see all Default").performClick()
        composeRule.onNodeWithText("reviews e1 Default").assertExists()

        tapTab("Profile")
        composeRule.onNodeWithText("Saved Experts").performScrollTo().performClick()
        composeRule.onNodeWithText("open favorite profile").performClick()
        composeRule.onNodeWithText("see all Profile").performClick()
        composeRule.onNodeWithText("reviews e1 Profile").assertExists()

        tapTab("Explore")
        composeRule.onNodeWithText("reviews e1 Default").assertExists()
        tapTab("Profile")
        composeRule.onNodeWithText("reviews e1 Profile").assertExists()
    }

    @Test
    fun clientReviewsRouteKeepsTheBarAndBackPops() {
        setClientWithProfileAndReviewsStandIns()
        composeRule.onNodeWithText("open explore profile").performClick()
        composeRule.onNodeWithText("see all Default").performClick()
        composeRule.onNodeWithText("reviews e1 Default").assertExists()
        composeRule.onNode(tab("Explore")).assertExists()

        pressSystemBack()

        composeRule.onNodeWithText("reviews e1 Default").assertDoesNotExist()
        composeRule.onNodeWithText("profile e1 Default").assertExists()
    }

    @Test
    fun expertReviewsRouteHidesTheBarAndBackPops() {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    dashboardRoot = { navigator ->
                        Button(onClick = { navigator.navigate(ExpertReviewsKey("e1", "Ada")) }) {
                            Text("open own reviews")
                        }
                    },
                    detailEntries = { navigator ->
                        expertReviewsEntry(navigator, metadata = BottomBar.hidden()) { key, _ ->
                            Text("reviews ${key.expertId} ${key.expertName}")
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithText("open own reviews").performClick()
        composeRule.onNodeWithText("reviews e1 Ada").assertExists()
        composeRule.onNode(tab("Dashboard")).assertDoesNotExist()

        pressSystemBack()

        composeRule.onNodeWithText("reviews e1 Ada").assertDoesNotExist()
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
    }

    @Test
    fun backToHomeResetsExploreToRoot() {
        openRequestSent()
        composeRule.onNodeWithText("sent home").performClick()
        composeRule.onNodeWithText("open booking", substring = true).assertExists()
        composeRule.onNode(tab("Explore")).assertIsSelected()
    }

    private fun setClientWithBookingManagement() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Column {
                            Text("fake explore")
                            Button(onClick = { navigator.openUpcomingBooking("b1") }) { Text("explore open upcoming") }
                        }
                    },
                    bookingsRoot = { navigator ->
                        Column {
                            Text("fake my bookings")
                            Button(onClick = { navigator.openUpcomingBooking("b1") }) { Text("open upcoming") }
                            Button(onClick = { navigator.openBookingSummary("b2") }) { Text("open summary") }
                        }
                    },
                    detailEntries = { nav ->
                        clientBookingFlowEntries(
                            navigator = nav,
                            booking = { _, onBack, _, _ ->
                                Column {
                                    Text("fake booking form")
                                    Button(onClick = onBack) { Text("fake back") }
                                }
                            },
                            requestSent = { _, _, _, _ -> Text("fake request sent") },
                        )
                        clientBookingManagementEntries(
                            navigator = nav,
                            upcoming = { key, onBack, onCancel, onOpenPaymentMethods, onReschedule ->
                                Column {
                                    Text("fake upcoming ${key.bookingId}")
                                    Button(onClick = onBack) { Text("upcoming back") }
                                    Button(onClick = onCancel) { Text("upcoming cancel") }
                                    Button(onClick = onOpenPaymentMethods) { Text("upcoming add card") }
                                    Button(onClick = onReschedule) { Text("upcoming reschedule") }
                                }
                            },
                            cancel = { key, onBack, onCancelled ->
                                Column {
                                    Text("fake cancel ${key.bookingId}")
                                    Button(onClick = onBack) { Text("cancel back") }
                                    Button(onClick = onCancelled) { Text("cancel done") }
                                }
                            },
                            summary = { key, onBack, onBookAgain, onOpenPaymentMethods, onLeaveReview ->
                                Column {
                                    Text("fake summary ${key.bookingId}")
                                    Button(onClick = { onLeaveReview(WriteReviewKey(key.bookingId)) }) {
                                        Text("summary leave review")
                                    }
                                    Button(onClick = onBack) { Text("summary back") }
                                    Button(
                                        onClick = { onBookAgain(BookingKey("e1", "Ada Lovelace", "Engineer", null)) },
                                    ) { Text("summary book again") }
                                    Button(onClick = onOpenPaymentMethods) { Text("summary add card") }
                                }
                            },
                            reschedule = { key, onBack, onOpenPaymentMethods ->
                                Column {
                                    Text("reschedule-stub:${key.bookingId}")
                                    Button(onClick = onBack) { Text("reschedule back") }
                                    Button(onClick = onOpenPaymentMethods) { Text("reschedule add card") }
                                }
                            },
                        )
                        fakePaymentMethodsEntry(nav)
                        entry<WriteReviewKey>(metadata = BottomBar.hidden()) {
                            Column {
                                Text("fake write review ${it.bookingId}")
                                Button(onClick = { nav.goBack() }) { Text("review back") }
                            }
                        }
                    },
                )
            }
        }
    }

    @Test
    fun bookingsTabShowsTheBookingsRootSlot() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("fake my bookings").assertExists()
    }

    @Test
    fun defaultShellKeepsThePlaceholderOnTheBookingsTab() {
        setClient(showDebug = false)
        tapTab("Bookings")
        composeRule.onNodeWithText("fake my bookings").assertDoesNotExist()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun upcomingRouteHidesBottomBarAndBackRestoresTheListAndBar() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open upcoming").performClick()
        composeRule.onNodeWithText("fake upcoming b1").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("upcoming back").performClick()
        composeRule.onNodeWithText("fake my bookings").assertExists()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
    }

    @Test
    fun summaryRouteOpensFromAPastCardWithoutTheBottomBar() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open summary").performClick()
        composeRule.onNodeWithText("fake summary b2").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("summary back").performClick()
        composeRule.onNodeWithText("fake my bookings").assertExists()
    }

    @Test
    fun cancelOpensWithoutBarAndCancelledReturnsToMyBookings() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open upcoming").performClick()
        composeRule.onNodeWithText("upcoming cancel").performClick()
        composeRule.onNodeWithText("fake cancel b1").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("cancel done").performClick()
        composeRule.onNodeWithText("fake my bookings").assertExists()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        composeRule.onNodeWithText("fake cancel b1").assertDoesNotExist()
        composeRule.onNodeWithText("fake upcoming b1").assertDoesNotExist()
    }

    @Test
    fun cancelledFromExploreOriginResetsExploreAndSelectsBookings() {
        setClientWithBookingManagement()
        composeRule.onNodeWithText("explore open upcoming").performClick()
        composeRule.onNodeWithText("fake upcoming b1").assertExists()
        composeRule.onNodeWithText("upcoming cancel").performClick()
        composeRule.onNodeWithText("cancel done").performClick()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        composeRule.onNodeWithText("fake my bookings").assertExists()
        tapTab("Explore")
        composeRule.onNodeWithText("fake explore").assertExists()
        composeRule.onNodeWithText("fake upcoming b1").assertDoesNotExist()
    }

    @Test
    fun cancelBackReturnsToTheUpcomingDetails() {
        setClientWithBookingManagement()
        composeRule.onNodeWithText("explore open upcoming").performClick()
        composeRule.onNodeWithText("upcoming cancel").performClick()
        composeRule.onNodeWithText("cancel back").performClick()
        composeRule.onNodeWithText("fake upcoming b1").assertExists()
    }

    @Test
    fun rescheduleKeyHidesBottomBarAndPops() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open upcoming").performClick()
        composeRule.onNodeWithText("upcoming reschedule").performClick()
        composeRule.onNodeWithText("reschedule-stub:b1").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("reschedule back").performClick()
        composeRule.onNodeWithText("reschedule-stub:b1").assertDoesNotExist()
        composeRule.onNodeWithText("fake upcoming b1").assertExists()
    }

    @Test
    fun rescheduleOpenPaymentMethodsPushesOnTop() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open upcoming").performClick()
        composeRule.onNodeWithText("upcoming reschedule").performClick()
        composeRule.onNodeWithText("reschedule add card").performClick()
        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.onNodeWithText("reschedule-stub:b1").assertExists()
    }

    @Test
    fun upcomingOnReschedulePushesPickerWithOriginAndOpener() {
        var seen: RescheduleKey? = null
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(UpcomingBookingKey("b1", StackOrigin.Chats)) }) {
                            Text("open chat origin upcoming")
                        }
                    },
                    detailEntries = { nav ->
                        clientBookingManagementEntries(
                            navigator = nav,
                            upcoming = { _, _, _, _, onReschedule ->
                                Button(onClick = onReschedule) { Text("upcoming reschedule") }
                            },
                            cancel = { _, _, _ -> },
                            summary = { _, _, _, _, _ -> },
                            reschedule = { key, _, _ ->
                                seen = key
                                Text("reschedule-stub:${key.bookingId}")
                            },
                        )
                    },
                )
            }
        }
        composeRule.onNodeWithText("open chat origin upcoming").performClick()
        composeRule.onNodeWithText("upcoming reschedule").performClick()
        composeRule.onNodeWithText("reschedule-stub:b1").assertExists()
        assertEquals(RescheduleKey("b1", RescheduleOpener.UpcomingDetails, StackOrigin.Chats), seen)
    }

    @Test
    fun upcomingAddCardOpensPaymentMethodsAndBackReturnsToTheSameBooking() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open upcoming").performClick()
        composeRule.onNodeWithText("upcoming add card").performClick()
        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.onNodeWithText("fake upcoming b1").assertExists()
    }

    @Test
    fun summaryAddCardOpensPaymentMethodsAndBackReturnsToTheSameBooking() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open summary").performClick()
        composeRule.onNodeWithText("summary add card").performClick()
        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.onNodeWithText("fake summary b2").assertExists()
    }

    @Test
    fun summaryLeaveReviewPushesWriteReview() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open summary").performClick()
        composeRule.onNodeWithText("summary leave review").performClick()
        composeRule.onNodeWithText("fake write review b2").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        composeRule.onNodeWithText("review back").performClick()
        composeRule.onNodeWithText("fake summary b2").assertExists()
    }

    @Test
    fun summaryBookAgainOpensTheBookingFormInTheSameStack() {
        setClientWithBookingManagement()
        tapTab("Bookings")
        composeRule.onNodeWithText("open summary").performClick()
        composeRule.onNodeWithText("summary book again").performClick()
        composeRule.onNodeWithText("fake booking form").assertExists()
        composeRule.onNodeWithText("fake back").performClick()
        composeRule.onNodeWithText("fake summary b2").assertExists()
    }

    @Test
    fun detailEntriesRouteKeepsBottomBar() {
        setClientWithExploreButton(
            label = "open profile",
            target = { it.navigate(ExpertProfileKey("e7", "Ada", "Engineer", null)) },
            detail = { entry<ExpertProfileKey> { Text("profile ${it.expertId}") } },
        )
        composeRule.onNodeWithText("open profile", substring = true).performClick()
        composeRule.onNodeWithText("profile e7").assertExists()
        composeRule.onNode(tab("Explore")).assertExists()
    }

    @Test
    fun exploreRootReceivesShellAvatarUrl() {
        setClientWithExploreButton(label = "root", target = {})
        composeRule.onNodeWithText("root https://example.com/me.png").assertExists()
    }

    @Test
    fun profilePaymentRowSitsAboveLogOutAndOpensPaymentMethodsWithoutTheBar() {
        setClient(showDebug = false, paymentMethods = true)
        tapTab("Profile")
        val rowTop = topOf("Payment")
        val logOutTop = topOf("Log out")
        assertTrue("Payment ($rowTop) must be above Log out ($logOutTop)", rowTop < logOutTop)

        composeRule.onNodeWithText("Payment").performScrollTo().performClick()

        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNode(tab("Profile")).assertDoesNotExist()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.onNodeWithText("fake payment methods").assertDoesNotExist()
        composeRule.onNode(tab("Profile")).assertIsSelected()
        composeRule.onNodeWithText("Payment").assertExists()
    }

    @Test
    fun bookingGateOpensPaymentMethodsOnTheExploreStackAndBackReturnsToTheForm() {
        setClientWithBookingFlow()
        composeRule.onNodeWithText("open booking", substring = true).performClick()
        composeRule.onNodeWithText("fake add card").performClick()

        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()

        composeRule.onNodeWithText("pm back").performClick()

        composeRule.onNodeWithText("fake payment methods").assertDoesNotExist()
        composeRule.onNodeWithText("fake add card").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        composeRule.onNodeWithText("fake back").performClick()
        composeRule.onNodeWithText("open booking", substring = true).assertExists()
        composeRule.onNode(tab("Explore")).assertExists()
    }

    /**
     * RESEARCH A10: the covered booking entry re-enters composition when Payment Methods is popped, so a
     * `LaunchedEffect(Unit)` refresh runs again. Whether ON_RESUME also reaches it is recorded, not asserted
     * (see the plan 07-12 SUMMARY).
     */
    @Test
    fun theCoveredBookingEntryReentersCompositionWhenPaymentMethodsIsPopped() {
        setClientWithBookingFlow()
        composeRule.onNodeWithText("open booking", substring = true).performClick()
        composeRule.waitForIdle()
        val enteredBefore = bookingEntered
        val resumedBefore = bookingResumed
        assertEquals(1, enteredBefore)

        composeRule.onNodeWithText("fake add card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.waitForIdle()

        val reentered = bookingEntered - enteredBefore
        val resumed = bookingResumed - resumedBefore
        println("A10 observed: LaunchedEffect runs +$reentered, ON_RESUME +$resumed")
        assertEquals(enteredBefore + 1, bookingEntered)
    }

    private var expertBackDispatcher: OnBackPressedDispatcher? = null

    private fun setExpertWithSlots(
        topicsRoot: @Composable (Navigator) -> Unit,
        detail: EntryProviderScope<NavKey>.(Navigator) -> Unit,
    ) {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    topicsRoot = topicsRoot,
                    detailEntries = detail,
                )
            }
        }
    }

    private fun EntryProviderScope<NavKey>.fakeAvailabilityEntry(navigator: Navigator) {
        entry<AvailabilityKey>(metadata = BottomBar.hidden()) { key ->
            Column {
                Text("availability-stub:${key.origin}")
                Button(onClick = navigator::goBack) { Text("availability back") }
            }
        }
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread { expertBackDispatcher?.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun defaultExpertShellKeepsThePlaceholderOnTheTopicsTab() {
        setExpert(showDebug = false)
        tapTab("Topics")
        composeRule.onNode(tab("Topics")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun availabilityRowStillOpensAvailability() {
        setExpertWithSlots(topicsRoot = { Text("fake topics") }, detail = { fakeAvailabilityEntry(it) })
        tapTab("Profile")
        val row = composeRule.onNode(hasTestTag("profile_availability_row"))
        row.assertExists()
        composeRule.onNodeWithText("Availability").assertExists()
        assertTrue(topOf("Availability") < topOf("Log out"))
        row.performScrollTo().performClick()
        composeRule.onNodeWithText("availability-stub:Profile").assertExists()
        composeRule.onNode(tab("Profile")).assertDoesNotExist()
        composeRule.onNodeWithText("availability back").performClick()
        composeRule.onNode(tab("Profile")).assertIsSelected()
        composeRule.onNodeWithText("availability-stub:Profile").assertDoesNotExist()
    }

    @Test
    fun topicsRootSlotRendersAndOpensAvailabilityFromTheTopicsStack() {
        setExpertWithSlots(
            topicsRoot = { navigator ->
                Column {
                    Text("fake topics")
                    Button(onClick = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Topics)) }) {
                        Text("open availability")
                    }
                }
            },
            detail = { fakeAvailabilityEntry(it) },
        )
        tapTab("Topics")
        composeRule.onNodeWithText("fake topics").assertExists()
        composeRule.onNodeWithText("open availability").performClick()
        composeRule.onNodeWithText("availability-stub:Topics").assertExists()
        composeRule.onNode(tab("Topics")).assertDoesNotExist()
        pressSystemBack()
        composeRule.onNodeWithText("fake topics").assertExists()
        composeRule.onNode(tab("Topics")).assertIsSelected()
    }

    @Test
    fun expertDashboardRootSlotRendersSuppliedContent() {
        composeRule.setContent {
            ClickMeTheme {
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    dashboardRoot = { Text("dash-slot") },
                )
            }
        }
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
        composeRule.onNodeWithText("dash-slot").assertExists()
        composeRule.onNodeWithText("Nothing here yet.").assertDoesNotExist()
    }

    @Test
    fun expertBookingsRootSlotRendersSuppliedContent() {
        composeRule.setContent {
            ClickMeTheme {
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    bookingsRoot = { Text("bookings-slot") },
                )
            }
        }
        composeRule.onNodeWithText("bookings-slot").assertDoesNotExist()
        tapTab("Bookings")
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        composeRule.onNodeWithText("bookings-slot").assertExists()
    }

    @Test
    fun topicEditorRouteHidesTheBarAndBackReturnsToTheTopicsList() {
        setExpertWithSlots(
            topicsRoot = { navigator ->
                Column {
                    Text("fake topics")
                    Button(onClick = { navigator.navigate(TopicEditorKey.newTopic()) }) { Text("add topic") }
                }
            },
            detail = { navigator ->
                entry<TopicEditorKey>(metadata = BottomBar.hidden()) {
                    Column {
                        Text("editor-stub")
                        Button(onClick = navigator::goBack) { Text("editor back") }
                    }
                }
            },
        )
        tapTab("Topics")
        composeRule.onNodeWithText("add topic").performClick()
        composeRule.onNodeWithText("editor-stub").assertExists()
        composeRule.onNode(tab("Topics")).assertDoesNotExist()
        pressSystemBack()
        composeRule.onNodeWithText("editor-stub").assertDoesNotExist()
        composeRule.onNodeWithText("fake topics").assertExists()
        composeRule.onNode(tab("Topics")).assertIsSelected()
    }

    /**
     * Stand-in for the thread route, registered with the real hidden-bar metadata. The real `chatEntries` is not
     * used: the thread route gets a Hilt ViewModel that Robolectric cannot build.
     */
    private fun EntryProviderScope<NavKey>.fakeChatThreadEntry() {
        entry<ChatThreadKey>(metadata = BottomBar.hidden()) { key ->
            Text(key.peerName?.takeIf { it.isNotBlank() } ?: "Chat")
        }
    }

    @Composable
    private fun pendingThreadOf(launch: ChatLaunchRequest?): ChatThreadKey? {
        if (launch == null) return null
        val pending by launch.thread.pending.collectAsState()
        return pending
    }

    private fun thread(
        id: String,
        name: String,
    ) = ChatThreadKey(id, "p-$id", name, null, true)

    @Test
    fun clientMessageHandOffOpensThreadOnChatsTab() {
        val launch = ChatLaunchRequest()
        setClient(showDebug = false, chatLaunch = launch)
        launch.thread.post(thread("t1", "Ada Lovelace"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ada Lovelace").assertExists()
        composeRule.onNode(tab("Chats")).assertDoesNotExist()
        assertNull(launch.thread.pending.value)
    }

    @Test
    fun backFromThreadLandsOnChatsRoot() {
        val launch = ChatLaunchRequest()
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                val pendingThread = pendingThreadOf(launch)
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(ExpertProfileKey("e7", "Ada", "Engineer", null)) }) {
                            Text("explore open profile")
                        }
                    },
                    detailEntries = {
                        entry<ExpertProfileKey> { Text("profile ${it.expertId}") }
                        fakeChatThreadEntry()
                    },
                    pendingChatThread = pendingThread,
                    onConsumeChatThread = launch.thread::consume,
                )
            }
        }
        composeRule.onNodeWithText("explore open profile").performClick()
        composeRule.onNodeWithText("profile e7").assertExists()
        launch.thread.post(thread("t1", "Ada Lovelace"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ada Lovelace").assertExists()
        pressSystemBack()
        composeRule.onNodeWithText("Ada Lovelace").assertDoesNotExist()
        composeRule.onNode(tab("Chats")).assertIsSelected()
        tapTab("Explore")
        composeRule.onNodeWithText("profile e7").assertExists()
    }

    @Test
    fun expertMessageHandOffOpensThreadOnChatsTab() {
        val launch = ChatLaunchRequest()
        setExpert(showDebug = false, chatLaunch = launch)
        launch.thread.post(thread("t1", "Ada Lovelace"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ada Lovelace").assertExists()
        composeRule.onNode(tab("Chats")).assertDoesNotExist()
        assertNull(launch.thread.pending.value)
        pressSystemBack()
        composeRule.onNode(tab("Chats")).assertIsSelected()
    }

    @Test
    fun secondHandOffReplacesThread() {
        val launch = ChatLaunchRequest()
        setClient(showDebug = false, chatLaunch = launch)
        launch.thread.post(thread("t1", "Ada Lovelace"))
        composeRule.waitForIdle()
        launch.thread.post(thread("t2", "Grace Hopper"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grace Hopper").assertExists()
        pressSystemBack()
        composeRule.onNodeWithText("Ada Lovelace").assertDoesNotExist()
        composeRule.onNodeWithText("Grace Hopper").assertDoesNotExist()
        composeRule.onNode(tab("Chats")).assertIsSelected()
    }

    @Composable
    private fun pendingRouteOf(routes: ShellRouteRequest?): ShellRoute? {
        if (routes == null) return null
        val pending by routes.pending.collectAsState()
        return pending
    }

    /**
     * Stand-ins for the routes a push can land on, registered with the same hidden-bar metadata as the real ones.
     * The real routes are not used: they get Hilt ViewModels that Robolectric cannot build.
     */
    private fun EntryProviderScope<NavKey>.pushStandIns() {
        entry<BookingSummaryKey> { Text("summary-stub:${it.bookingId}") }
        entry<UpcomingBookingKey> { Text("upcoming-stub:${it.bookingId}") }
        entry<ExpertBookingSummaryKey> { Text("expert-summary-stub:${it.bookingId}") }
        entry<IncomingRequestsKey> { Text("incoming-stub") }
        entry<RequestDecisionKey> { Text("decision-stub:${it.requestId}") }
        entry<NotificationsKey>(metadata = BottomBar.hidden()) { Text("notifications-stub:${it.origin}") }
        entry<WriteReviewKey>(metadata = BottomBar.hidden()) { Text("write-review-stub:${it.bookingId}") }
    }

    private fun noteKey(origin: StackOrigin = StackOrigin.Default) =
        ClientBookingNoteKey("b1", "Ada", "Algorithms", 0L, 3_600_000L, origin)

    /**
     * A Bookings root that pushes the Default note key, a Chats root stand-in, and the real [clientBookingNoteEntry]
     * with a stand-in editor, so the real hidden-bar metadata and back callback run (D-05, D-06).
     */
    private fun setClientWithNoteStandIn(routes: ShellRouteRequest) {
        composeRule.setContent {
            ClickMeTheme {
                expertBackDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                val pendingRoute = pendingRouteOf(routes)
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    bookingsRoot = { navigator ->
                        Button(onClick = { navigator.navigate(noteKey()) }) { Text("open note") }
                    },
                    chatsRoot = { Text("chats root") },
                    detailEntries = { navigator ->
                        clientBookingNoteEntry(navigator) { key, _ -> Text("note ${key.bookingId} ${key.origin}") }
                    },
                    pendingShellRoute = pendingRoute,
                    onConsumeShellRoute = { routes.consumeFor(AppMode.Client) },
                )
            }
        }
    }

    @Test
    fun clientNoteRouteHidesTheBarAndBackPops() {
        setClientWithNoteStandIn(ShellRouteRequest())
        tapTab("Bookings")
        composeRule.onNodeWithText("open note").performClick()
        composeRule.onNodeWithText("note b1 Default").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()

        pressSystemBack()

        composeRule.onNodeWithText("note b1 Default").assertDoesNotExist()
        composeRule.onNodeWithText("open note").assertExists()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
    }

    @Test
    fun noteOpenOnBookingsAndChatsDoesNotCollide() {
        val routes = ShellRouteRequest()
        setClientWithNoteStandIn(routes)
        tapTab("Bookings")
        composeRule.onNodeWithText("open note").performClick()
        composeRule.onNodeWithText("note b1 Default").assertExists()

        routes.post(ShellRoute(AppMode.Client, ClientChatsRoot, listOf(noteKey(StackOrigin.Chats))))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("note b1 Chats").assertExists()

        pressSystemBack()
        composeRule.onNodeWithText("chats root").assertExists()
        composeRule.onNodeWithText("note b1 Chats").assertDoesNotExist()

        tapTab("Bookings")
        composeRule.onNodeWithText("note b1 Default").assertExists()
    }

    @Test
    fun postedClientRouteOpensSummaryOnBookingsTab() {
        val routes = ShellRouteRequest()
        setClient(showDebug = false, shellRoutes = routes)
        routes.post(ShellRoute(AppMode.Client, ClientBookingsRoot, listOf(BookingSummaryKey("B1"))))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("summary-stub:B1").assertExists()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        assertNull(routes.pending.value)
        pressSystemBack()
        composeRule.onNodeWithText("summary-stub:B1").assertDoesNotExist()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
    }

    @Test
    fun expertRouteIsLeftForTheExpertShell() {
        val routes = ShellRouteRequest()
        setClient(showDebug = false, shellRoutes = routes)
        val route =
            ShellRoute(
                AppMode.Expert,
                ExpertBookingsRoot,
                listOf(ExpertBookingSummaryKey("B1", ExpertBookingOrigin.Bookings)),
            )
        routes.post(route)
        composeRule.waitForIdle()
        composeRule.onNode(tab("Explore")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
        assertEquals(route, routes.pending.value)
    }

    @Test
    fun expertShellConsumesItsRoute() {
        val routes = ShellRouteRequest()
        setExpert(showDebug = false, shellRoutes = routes)
        routes.post(
            ShellRoute(
                AppMode.Expert,
                ExpertBookingsRoot,
                listOf(ExpertBookingSummaryKey("B1", ExpertBookingOrigin.Bookings)),
            ),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("expert-summary-stub:B1").assertExists()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        assertNull(routes.pending.value)
    }

    @Test
    fun expertRequestChainBacksToListThenDashboard() {
        val routes = ShellRouteRequest()
        setExpert(showDebug = false, shellRoutes = routes)
        routes.post(
            ShellRoute(AppMode.Expert, DashboardRoot, listOf(IncomingRequestsKey, RequestDecisionKey("r1"))),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("decision-stub:r1").assertExists()
        pressSystemBack()
        composeRule.onNodeWithText("decision-stub:r1").assertDoesNotExist()
        composeRule.onNodeWithText("incoming-stub").assertExists()
        pressSystemBack()
        composeRule.onNodeWithText("incoming-stub").assertDoesNotExist()
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun payoutsPushReusesPayoutsOpenedFromTheHub() {
        val routes = ShellRouteRequest()
        setExpert(showDebug = false, hubStandIns = true, shellRoutes = routes)
        tapTab("Profile")
        composeRule.onNodeWithText("Payouts & Earnings").performScrollTo().performClick()
        composeRule.onNodeWithText("stand-in payouts:Profile").assertExists()

        routes.post(ShellRoute(AppMode.Expert, DashboardRoot, listOf(PayoutsKey(SettingsOrigin.Dashboard))))
        composeRule.waitForIdle()

        // The open instance is shown; no second Payouts was stacked on the Dashboard tab.
        composeRule.onNodeWithText("stand-in payouts:Profile").assertExists()
        composeRule.onNodeWithText("stand-in payouts:Dashboard").assertDoesNotExist()
        assertNull(routes.pending.value)
        pressSystemBack()
        composeRule.onNode(tab("Profile")).assertIsSelected()
    }

    @Test
    fun notificationsKeysDifferPerStack() {
        assertNotEquals(
            NotificationsKey(NotificationsOrigin.ClientExplore),
            NotificationsKey(NotificationsOrigin.ExpertDashboard),
        )
        assertEquals(NotificationsOrigin.ClientExplore, notificationsKeyFor(AppMode.Client).origin)
        assertEquals(NotificationsOrigin.ExpertDashboard, notificationsKeyFor(AppMode.Expert).origin)
        assertEquals(ExploreRoot, notificationsRootFor(AppMode.Client))
        assertEquals(DashboardRoot, notificationsRootFor(AppMode.Expert))
    }

    @Test
    fun notificationsRouteOnExploreHidesBottomBar() {
        val routes = ShellRouteRequest()
        setClient(showDebug = false, shellRoutes = routes)
        routes.post(ShellRoute(AppMode.Client, ExploreRoot, listOf(notificationsKeyFor(AppMode.Client))))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("notifications-stub:ClientExplore").assertExists()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        pressSystemBack()
        composeRule.onNodeWithText("notifications-stub:ClientExplore").assertDoesNotExist()
        composeRule.onNode(tab("Explore")).assertIsSelected()
    }

    @Test
    fun writeReviewRouteHidesBottomBarOnTheBookingsStack() {
        val routes = ShellRouteRequest()
        setClient(showDebug = false, shellRoutes = routes)
        routes.post(
            ShellRoute(
                AppMode.Client,
                ClientBookingsRoot,
                listOf(BookingSummaryKey("B1"), WriteReviewKey("B1")),
            ),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("write-review-stub:B1").assertExists()
        composeRule.onNode(tab("Bookings")).assertDoesNotExist()
        pressSystemBack()
        composeRule.onNodeWithText("summary-stub:B1").assertExists()
    }
}

private const val SEARCH_STAND_IN_FIELD_TAG = "stand-in query"

private class SearchStandInViewModel : ViewModel() {
    var text by mutableStateOf("")
}
