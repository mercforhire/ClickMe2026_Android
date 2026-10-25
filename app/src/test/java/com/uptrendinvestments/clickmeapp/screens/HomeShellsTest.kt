package com.uptrendinvestments.clickmeapp.screens

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.ClientHomeGate
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientShell
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientBookingFlowEntries
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientBookingManagementEntries
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.clientPaymentMethodsEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openBookingSummary
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openUpcomingBooking
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertShell
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorKey
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    ) {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onSwitchMode = { switched += it },
                    onLogout = { logoutCount++ },
                    onDebugForceRefresh = { refreshCount++ },
                    debugRefreshStatus = refreshStatus,
                    detailEntries = { navigator -> if (paymentMethods) fakePaymentMethodsEntry(navigator) },
                )
            }
        }
    }

    private fun setExpert(
        showDebug: Boolean,
        refreshStatus: String? = null,
    ) {
        composeRule.setContent {
            ClickMeTheme {
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onSwitchMode = { switched += it },
                    onLogout = { logoutCount++ },
                    onDebugForceRefresh = { refreshCount++ },
                    debugRefreshStatus = refreshStatus,
                )
            }
        }
    }

    // Tab names also appear as TopAppBar titles and empty-state headings, so match the bar item.
    private fun tab(label: String) = hasText(label) and isSelectable()

    private fun topOf(text: String): Float {
        val node = composeRule.onNodeWithText(text).fetchSemanticsNode()
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
    fun clientProfileShowsLogOutInReleaseAndInvokesCallback() {
        setClient(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Force token refresh").assertDoesNotExist()
        composeRule.onNodeWithText("Log out").performClick()
        assertEquals(1, logoutCount)
    }

    @Test
    fun expertProfileShowsLogOutInReleaseAndInvokesCallback() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Force token refresh").assertDoesNotExist()
        composeRule.onNodeWithText("Log out").performClick()
        assertEquals(1, logoutCount)
    }

    @Test
    fun clientDebugProfileOffersForceRefreshAndShowsStatus() {
        setClient(showDebug = true, refreshStatus = "Token refresh OK")
        tapTab("Profile")
        composeRule.onNodeWithText("Log out").assertExists()
        composeRule.onNodeWithText("Token refresh OK").assertExists()
        composeRule.onNodeWithText("Force token refresh").performScrollTo().performClick()
        assertEquals(1, refreshCount)
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
                            upcoming = { key, onBack, onCancel, onOpenPaymentMethods ->
                                Column {
                                    Text("fake upcoming ${key.bookingId}")
                                    Button(onClick = onBack) { Text("upcoming back") }
                                    Button(onClick = onCancel) { Text("upcoming cancel") }
                                    Button(onClick = onOpenPaymentMethods) { Text("upcoming add card") }
                                }
                            },
                            cancel = { key, onBack, onCancelled ->
                                Column {
                                    Text("fake cancel ${key.bookingId}")
                                    Button(onClick = onBack) { Text("cancel back") }
                                    Button(onClick = onCancelled) { Text("cancel done") }
                                }
                            },
                            summary = { key, onBack, onBookAgain, onOpenPaymentMethods ->
                                Column {
                                    Text("fake summary ${key.bookingId}")
                                    Button(onClick = onBack) { Text("summary back") }
                                    Button(
                                        onClick = { onBookAgain(BookingKey("e1", "Ada Lovelace", "Engineer", null)) },
                                    ) { Text("summary book again") }
                                    Button(onClick = onOpenPaymentMethods) { Text("summary add card") }
                                }
                            },
                        )
                        fakePaymentMethodsEntry(nav)
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
    fun profileShowsPaymentMethodsAboveLogOutAndOpensItWithoutTheBar() {
        setClient(showDebug = false, paymentMethods = true)
        tapTab("Profile")
        val rowTop = topOf("Payment Methods")
        val logOutTop = topOf("Log out")
        assertTrue("Payment Methods ($rowTop) must be above Log out ($logOutTop)", rowTop < logOutTop)

        composeRule.onNodeWithText("Payment Methods").performScrollTo().performClick()

        composeRule.onNodeWithText("fake payment methods").assertExists()
        composeRule.onNode(tab("Profile")).assertDoesNotExist()
        composeRule.onNodeWithText("pm back").performClick()
        composeRule.onNodeWithText("fake payment methods").assertDoesNotExist()
        composeRule.onNode(tab("Profile")).assertIsSelected()
        composeRule.onNodeWithText("Payment Methods").assertExists()
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
    fun expertProfileAvailabilityRowSitsAboveLogOutAndOpensAvailabilityWithoutTheBar() {
        setExpertWithSlots(topicsRoot = { Text("fake topics") }, detail = { fakeAvailabilityEntry(it) })
        tapTab("Profile")
        val row = composeRule.onNode(hasTestTag("profile_availability_row"))
        row.assertExists()
        composeRule.onNodeWithText("Availability").assertExists()
        assertTrue(topOf("Availability") < topOf("Log out"))
        row.performClick()
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
}
