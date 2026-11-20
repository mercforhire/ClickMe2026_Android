package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PlaceholderTabContent
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.HomeTabScaffold
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.rememberNavigationState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.chatEntries
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.ConversationsRoute
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteRoute
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.clientBookingNoteEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancellationPolicyKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancellationPolicyScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.ClientCancellationScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.ClientCancellationViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.ExploreScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.clientFavoritesEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientStartTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientTabs
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.MakeABookingScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.MakeABookingViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.MyBookingsScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.payments.ClientTransactionsKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ClientProfileHomeContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ClientProfileHomeScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ClientProfileHubActions
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.profileHubVersionLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.BookingRequestSentScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.BookingRequestSentViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ExpertReviewsScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ExpertReviewsViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.expertReviewsEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.reviewsKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchExpertScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.writeReviewEntries
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryKey
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryScreen
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsEntries
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsKeyFor
import com.uptrendinvestments.clickmeapp.screens.onboarding.OnboardingContent
import com.uptrendinvestments.clickmeapp.screens.push.ShellRoute
import com.uptrendinvestments.clickmeapp.screens.push.ShellRouteViewModel
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.clientSettingsEntries
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ClientProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.feedback.FeedbackKey
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchScreen
import com.uptrendinvestments.clickmeapp.screens.settings.notifications.NotificationSettingsKey
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsKey
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsScreen
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Hilt-aware entry for the client shell: supplies the avatar URL, session actions and the debug gate. */
@Composable
fun HomeClientScreen(
    onSwitchMode: (AppMode) -> Unit,
    viewModel: HomeClientViewModel = hiltViewModel(),
    chatLaunch: ChatLaunchViewModel = hiltViewModel(),
    shellRoute: ShellRouteViewModel = hiltViewModel(),
) {
    val pendingChatThread by chatLaunch.pendingThread.collectAsStateWithLifecycle()
    val pendingShellRoute by shellRoute.pending.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val debugRefreshStatus by viewModel.debugRefreshStatus.collectAsStateWithLifecycle()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    ClientHomeGate(
        showOnboarding = !hasSeenOnboarding,
        onboarding = { OnboardingContent(onFinish = viewModel::markOnboardingShown) },
        home = {
            HomeClientShell(
                avatarUrl = avatarUrl,
                showDebugAffordances = BuildConfig.DEBUG,
                onSwitchMode = onSwitchMode,
                onLogout = viewModel::logout,
                onDebugForceRefresh = viewModel::debugForceRefresh,
                debugRefreshStatus = debugRefreshStatus,
                exploreRoot = { navigator, url ->
                    ExploreScreen(
                        avatarUrl = url,
                        onAvatarClick = { navigator.navigate(ModeSwitchKey) },
                        onOpenExpert = { navigator.navigate(it) },
                        onOpenBooking = navigator::openUpcomingBooking,
                        onOpenNotifications = { navigator.navigate(notificationsKeyFor(AppMode.Client)) },
                        onSeeAll = navigator::showSearch,
                    )
                },
                searchRoot = { navigator ->
                    SearchExpertScreen(onOpenExpert = { navigator.navigate(it) })
                },
                bookingsRoot = { navigator ->
                    MyBookingsScreen(
                        onOpenUpcoming = navigator::openUpcomingBooking,
                        onOpenPast = navigator::openBookingSummary,
                        onExploreExperts = navigator::showExplore,
                        onLeaveReview = { navigator.navigate(it) },
                        onReschedule = { navigator.navigate(RescheduleKey(it, RescheduleOpener.MyBookings)) },
                        onPrivateNote = { navigator.navigate(it) },
                    )
                },
                chatsRoot = { navigator ->
                    ConversationsRoute(
                        navigator = navigator,
                        mode = AppMode.Client,
                        avatarUrl = avatarUrl,
                        onOpenModeSwitch = { navigator.navigate(ModeSwitchKey) },
                    )
                },
                profileRoot = { navigator, debug ->
                    ClientProfileHomeScreen(
                        actions = clientProfileHubActions(navigator, onSwitchMode, viewModel::logout),
                        versionLabel = profileHubVersionLabel(),
                        debugContent = debug,
                    )
                },
                detailEntries = { navigator ->
                    entry<ExpertProfileKey> { key ->
                        val profileViewModel =
                            hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key) },
                            )
                        ExpertProfileScreen(
                            viewModel = profileViewModel,
                            onBack = navigator::goBack,
                            onBook = { navigator.navigate(it) },
                            onSeeAllReviews = { name -> navigator.navigate(key.reviewsKey(name)) },
                        )
                    }
                    expertReviewsEntry(navigator) { key, onBack ->
                        ExpertReviewsScreen(
                            viewModel =
                                hiltViewModel<ExpertReviewsViewModel, ExpertReviewsViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(key) },
                                ),
                            onBack = onBack,
                        )
                    }
                    clientBookingFlowEntries(
                        navigator = navigator,
                        booking = { key, onBack, onBooked, onOpenPaymentMethods ->
                            val bookingViewModel =
                                hiltViewModel<MakeABookingViewModel, MakeABookingViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            MakeABookingScreen(
                                viewModel = bookingViewModel,
                                onBack = onBack,
                                onBooked = onBooked,
                                onOpenPaymentMethods = onOpenPaymentMethods,
                            )
                        },
                        requestSent = { key, onBack, onViewBookings, onBackToHome ->
                            val sentViewModel =
                                hiltViewModel<BookingRequestSentViewModel, BookingRequestSentViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            BookingRequestSentScreen(
                                viewModel = sentViewModel,
                                onBack = onBack,
                                onViewBookings = onViewBookings,
                                onBackToHome = onBackToHome,
                            )
                        },
                    )
                    clientBookingManagementEntries(
                        navigator = navigator,
                        upcoming = { key, onBack, onCancel, onOpenPaymentMethods, onReschedule ->
                            val upcomingViewModel =
                                hiltViewModel<UpcomingBookingViewModel, UpcomingBookingViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            UpcomingBookingScreen(
                                viewModel = upcomingViewModel,
                                onBack = onBack,
                                onCancel = onCancel,
                                onOpenPaymentMethods = onOpenPaymentMethods,
                                onReschedule = onReschedule,
                            )
                        },
                        cancel = { key, onBack, onCancelled ->
                            val cancelViewModel =
                                hiltViewModel<ClientCancellationViewModel, ClientCancellationViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            ClientCancellationScreen(
                                viewModel = cancelViewModel,
                                onBack = onBack,
                                onCancelled = onCancelled,
                                onOpenPolicy = { navigator.navigate(CancellationPolicyKey) },
                            )
                        },
                        summary = { key, onBack, onBookAgain, onOpenPaymentMethods, onLeaveReview, onOpenNote ->
                            val summaryViewModel =
                                hiltViewModel<BookingSummaryViewModel, BookingSummaryViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            BookingSummaryScreen(
                                viewModel = summaryViewModel,
                                onBack = onBack,
                                onBookAgain = onBookAgain,
                                onOpenPaymentMethods = onOpenPaymentMethods,
                                onLeaveReview = onLeaveReview,
                                onOpenNote = onOpenNote,
                            )
                        },
                        reschedule = { key, onBack, onOpenPaymentMethods ->
                            val rescheduleViewModel =
                                hiltViewModel<RescheduleViewModel, RescheduleViewModel.Factory>(
                                    creationCallback = { it.create(key) },
                                )
                            RescheduleScreen(
                                viewModel = rescheduleViewModel,
                                onBack = onBack,
                                onOpenPaymentMethods = onOpenPaymentMethods,
                            )
                        },
                    )
                    entry<CancellationPolicyKey>(metadata = BottomBar.hidden()) {
                        CancellationPolicyScreen(onBack = navigator::goBack)
                    }
                    clientFavoritesEntry(navigator) { actions ->
                        FavoritesScreen(
                            viewModel = hiltViewModel(),
                            onBack = actions.onBack,
                            onOpenExpert = actions.onOpenExpert,
                            onExplore = actions.onExplore,
                            onViewHistory = actions.onViewHistory,
                        )
                    }
                    clientPaymentMethodsEntry(navigator) { onBack ->
                        PaymentMethodsScreen(viewModel = hiltViewModel(), onBack = onBack)
                    }
                    chatEntries(navigator, AppMode.Client)
                    notificationsEntries(navigator, AppMode.Client)
                    writeReviewEntries(navigator)
                    clientBookingNoteEntry(navigator) { key, onBack -> ClientBookingNoteRoute(key, onBack) }
                    clientSettingsEntries(navigator)
                },
                pendingChatThread = pendingChatThread,
                onConsumeChatThread = chatLaunch::consumeThread,
                pendingShellRoute = pendingShellRoute,
                onConsumeShellRoute = { shellRoute.consumeFor(AppMode.Client) },
            )
        },
    )
}

/**
 * Shows the onboarding carousel while [showOnboarding] is true, else the client home (D-21). A plain
 * composable in the same window (no separate window), so the exit is a 200 ms cross-fade (UI-SPEC).
 */
@Composable
fun ClientHomeGate(
    showOnboarding: Boolean,
    onboarding: @Composable () -> Unit,
    home: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = showOnboarding,
        modifier = modifier,
        transitionSpec = { fadeIn(tween(ONBOARDING_FADE_MS)) togetherWith fadeOut(tween(ONBOARDING_FADE_MS)) },
        label = "client-home-gate",
    ) { showing ->
        if (showing) onboarding() else home()
    }
}

private const val ONBOARDING_FADE_MS = 200

/**
 * The client 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 *
 * Seven slots let [HomeClientScreen] inject Hilt-backed screens without the shell depending on Hilt:
 * [exploreRoot] renders the Explore tab root (default: the placeholder with the avatar button),
 * [searchRoot] renders the Search tab root (default: the placeholder, so Robolectric shell tests need no Hilt),
 * [bookingsRoot] renders the Bookings tab root (default: the placeholder), [chatsRoot] renders the Chats tab root
 * (default: the placeholder), [profileRoot] renders the Profile hub (default: the stateless hub with the real
 * navigation actions; the shell hands it the debug affordances to show below the version footer), and [detailEntries] registers extra
 * routes pushed on top of a tab, such as the Expert Profile, the booking flow routes (see
 * [clientBookingFlowEntries]), the booking-management routes (see [clientBookingManagementEntries]) and Payment
 * Methods (see [clientPaymentMethodsEntry]). [pendingChatThread] / [onConsumeChatThread] are the chat hand-off: when a
 * thread key is pending the shell consumes it and opens it on the Chats tab ([Navigator.openChatThread]).
 * [pendingShellRoute] / [onConsumeShellRoute] are the push route hand-off: a resolved client [ShellRoute] is consumed
 * only by this shell and inserted with [Navigator.openRouted].
 */
@Composable
fun HomeClientShell(
    avatarUrl: String?,
    showDebugAffordances: Boolean,
    onSwitchMode: (AppMode) -> Unit,
    onLogout: () -> Unit,
    onDebugForceRefresh: () -> Unit,
    debugRefreshStatus: String?,
    modifier: Modifier = Modifier,
    exploreRoot: @Composable (navigator: Navigator, avatarUrl: String?) -> Unit = { navigator, url ->
        ClientTabRoot(
            key = ExploreRoot,
            actions = { TopBarAvatarButton(url, onClick = { navigator.navigate(ModeSwitchKey) }) },
        )
    },
    searchRoot: @Composable (navigator: Navigator) -> Unit = { ClientTabRoot(SearchRoot) },
    bookingsRoot: @Composable (navigator: Navigator) -> Unit = { ClientTabRoot(ClientBookingsRoot) },
    chatsRoot: @Composable (navigator: Navigator) -> Unit = { ClientTabRoot(ClientChatsRoot) },
    profileRoot: @Composable (navigator: Navigator, debugContent: @Composable () -> Unit) -> Unit =
        { navigator, debugContent ->
            ClientProfileHomeContent(
                card = ProfileHubCard(name = null, email = null, avatarUrl = null),
                versionLabel = "",
                actions = clientProfileHubActions(navigator, onSwitchMode, onLogout),
                debugContent = debugContent,
            )
        },
    detailEntries: EntryProviderScope<NavKey>.(navigator: Navigator) -> Unit = {},
    pendingChatThread: ChatThreadKey? = null,
    onConsumeChatThread: () -> ChatThreadKey? = { null },
    pendingShellRoute: ShellRoute? = null,
    onConsumeShellRoute: () -> ShellRoute? = { null },
) {
    val state = rememberNavigationState(clientStartTab, clientTabs.map { it.key }.toSet())
    val navigator = remember(state) { Navigator(state) }
    val debugContent: @Composable () -> Unit = {
        if (showDebugAffordances) {
            ClientProfileDebugAffordances(
                onOpenGallery = { navigator.navigate(DesignSystemGalleryKey) },
                onDebugForceRefresh = onDebugForceRefresh,
                debugRefreshStatus = debugRefreshStatus,
            )
        }
    }

    // A Message tap, or the in-call Chat control, posts a thread. Consume the slot so it applies exactly once.
    LaunchedEffect(pendingChatThread) {
        if (pendingChatThread != null) {
            onConsumeChatThread()?.let { navigator.openChatThread(ClientChatsRoot, it) }
        }
    }

    // A resolved push route. Only the client shell consumes a client route: the outgoing shell of a mode switch is
    // still composed for the cross-fade and must leave an expert route alone.
    LaunchedEffect(pendingShellRoute) {
        if (pendingShellRoute?.mode == AppMode.Client) {
            onConsumeShellRoute()?.let { route ->
                val routable = route.tab in state.backStacks && route.keys.isNotEmpty()
                if (routable) navigator.openRouted(route.tab, route.keys)
            }
        }
    }

    HomeTabScaffold(
        tabs = clientTabs,
        navigationState = state,
        navigator = navigator,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<ExploreRoot> { exploreRoot(navigator, avatarUrl) }
                entry<SearchRoot> { searchRoot(navigator) }
                entry<ClientBookingsRoot> { bookingsRoot(navigator) }
                entry<ClientChatsRoot> { chatsRoot(navigator) }
                entry<ClientProfileRoot> { profileRoot(navigator, debugContent) }
                entry<ModeSwitchKey>(metadata = BottomBar.hidden()) {
                    ModeSwitchScreen(
                        currentMode = AppMode.Client,
                        onSwitchMode = onSwitchMode,
                        onBack = navigator::goBack,
                    )
                }
                entry<DesignSystemGalleryKey> {
                    DesignSystemGalleryScreen(onBack = navigator::goBack)
                }
                detailEntries(navigator)
            },
    )
}

/**
 * Explore "See all" (D-01): mirrors iOS `onSeeAllExperts: { selectedTab = .search }`. Selects the Search tab only; it
 * never resets that tab's stack and never passes Explore's filter. A re-tap of the Search tab is the bottom bar's
 * `reselect` (D-03).
 */
internal fun Navigator.showSearch() = navigate(SearchRoot)

/** Maps the client hub's callbacks to destinations (PROF-01); shared by the Hilt hub and the shell default. */
internal fun clientProfileHubActions(
    navigator: Navigator,
    onSwitchMode: (AppMode) -> Unit,
    onLogout: () -> Unit,
): ClientProfileHubActions =
    ClientProfileHubActions(
        onEditProfile = { navigator.navigate(ClientProfileSettingsKey) },
        onSwitchToExpert = { onSwitchMode(AppMode.Expert) },
        onOpenSavedExperts = { navigator.navigate(FavoritesKey) },
        onOpenSecurity = { navigator.navigate(AccountSecurityKey) },
        onOpenPayment = { navigator.navigate(PaymentMethodsKey) },
        onOpenPaymentHistory = { navigator.navigate(ClientTransactionsKey) },
        onOpenNotifications = { navigator.navigate(NotificationSettingsKey) },
        onOpenHelp = { navigator.navigate(HelpKey) },
        onOpenFeedback = { navigator.navigate(FeedbackKey) },
        onLogOut = onLogout,
    )

/** Debug-only rows below the hub's version footer; the shell renders them only when the debug gate is on. */
@Composable
private fun ClientProfileDebugAffordances(
    onOpenGallery: () -> Unit,
    onDebugForceRefresh: () -> Unit,
    debugRefreshStatus: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedButton(onClick = onOpenGallery) {
            Text(stringResource(R.string.debug_design_system_gallery))
        }
        OutlinedButton(onClick = onDebugForceRefresh) {
            Text(stringResource(R.string.debug_force_token_refresh))
        }
        if (debugRefreshStatus != null) {
            Text(debugRefreshStatus)
        }
    }
}

@Composable
private fun ClientTabRoot(
    key: NavKey,
    actions: @Composable RowScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val tab = clientTabs.first { it.key == key }
    PlaceholderTabContent(
        title = stringResource(tab.labelRes),
        icon = tab.icon,
        actions = actions,
        footer = footer,
    )
}
