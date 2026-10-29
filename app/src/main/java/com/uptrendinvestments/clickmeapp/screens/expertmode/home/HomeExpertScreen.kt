package com.uptrendinvestments.clickmeapp.screens.expertmode.home

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
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.navigation.rememberNavigationState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.chatEntries
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.ConversationsRoute
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileScreen
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileViewModel
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.ProfileHubCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.profile.subviews.profileHubVersionLabel
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryKey
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.expertBookingSummaryEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.expertCancelEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.bookingConfirmedEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertDashboardRoute
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertStartTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertTabs
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.incomingRequestsEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.profile.ExpertProfileHomeContent
import com.uptrendinvestments.clickmeapp.screens.expertmode.profile.ExpertProfileHomeScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.profile.ExpertProfileHubActions
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.requestDecisionEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertProfileSettingsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile.TaxProfileKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorViewModel
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRoute
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsEntries
import com.uptrendinvestments.clickmeapp.screens.push.ShellRoute
import com.uptrendinvestments.clickmeapp.screens.push.ShellRouteViewModel
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.screens.settings.expertSettingsEntries
import com.uptrendinvestments.clickmeapp.screens.settings.help.HelpKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchScreen
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * Hilt-aware entry for the expert shell: supplies the avatar URL, session actions and the debug gate.
 *
 * Phase 11 features register their routes from their own packages (`incomingRequestsEntries`,
 * `requestDecisionEntries`, `bookingConfirmedEntries`, `expertBookingSummaryEntries`, `expertCancelEntries`), and the
 * Dashboard and Bookings tab roots come from `ExpertDashboardRoute` / `ExpertBookingsRoute`, so a feature plan never
 * edits this file.
 */
@Composable
fun HomeExpertScreen(
    onSwitchMode: (AppMode) -> Unit,
    viewModel: HomeExpertViewModel = hiltViewModel(),
    chatLaunch: ChatLaunchViewModel = hiltViewModel(),
    shellRoute: ShellRouteViewModel = hiltViewModel(),
) {
    val pendingChatThread by chatLaunch.pendingThread.collectAsStateWithLifecycle()
    val pendingShellRoute by shellRoute.pending.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val debugRefreshStatus by viewModel.debugRefreshStatus.collectAsStateWithLifecycle()
    HomeExpertShell(
        avatarUrl = avatarUrl,
        showDebugAffordances = BuildConfig.DEBUG,
        onSwitchMode = onSwitchMode,
        onLogout = viewModel::logout,
        onDebugForceRefresh = viewModel::debugForceRefresh,
        debugRefreshStatus = debugRefreshStatus,
        dashboardRoot = { navigator ->
            ExpertDashboardRoute(
                navigator = navigator,
                avatarUrl = avatarUrl,
                onOpenModeSwitch = { navigator.navigate(ModeSwitchKey) },
            )
        },
        bookingsRoot = { navigator -> ExpertBookingsRoute(navigator) },
        topicsRoot = { navigator ->
            TopicsScreen(
                onAddTopic = { navigator.navigate(TopicEditorKey.newTopic()) },
                onEditTopic = { navigator.navigate(TopicEditorKey.forTopic(it)) },
                onOpenAvailability = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Topics)) },
            )
        },
        chatsRoot = { navigator ->
            ConversationsRoute(
                navigator = navigator,
                mode = AppMode.Expert,
                avatarUrl = avatarUrl,
                onOpenModeSwitch = { navigator.navigate(ModeSwitchKey) },
            )
        },
        profileRoot = { navigator, debug ->
            ExpertProfileHomeScreen(
                actions = expertProfileHubActions(navigator, onSwitchMode, viewModel::logout),
                onOpenPublicProfile = { navigator.navigate(it) },
                versionLabel = profileHubVersionLabel(),
                debugContent = debug,
            )
        },
        detailEntries = { navigator ->
            entry<TopicEditorKey>(metadata = BottomBar.hidden()) { key ->
                TopicEditorScreen(
                    viewModel =
                        hiltViewModel<TopicEditorViewModel, TopicEditorViewModel.Factory>(
                            creationCallback = { factory -> factory.create(key) },
                        ),
                    onBack = navigator::goBack,
                )
            }
            entry<AvailabilityKey>(metadata = BottomBar.hidden()) {
                AvailabilityScreen(viewModel = hiltViewModel(), onBack = navigator::goBack)
            }
            incomingRequestsEntries(navigator)
            requestDecisionEntries(navigator)
            bookingConfirmedEntries(navigator)
            expertBookingSummaryEntries(navigator)
            expertCancelEntries(navigator)
            chatEntries(navigator, AppMode.Expert)
            notificationsEntries(navigator, AppMode.Expert)
            expertProfileEntry(navigator)
            expertSettingsEntries(navigator)
        },
        pendingChatThread = pendingChatThread,
        onConsumeChatThread = chatLaunch::consumeThread,
        pendingShellRoute = pendingShellRoute,
        onConsumeShellRoute = { shellRoute.consumeFor(AppMode.Expert) },
    )
}

/**
 * The expert 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 *
 * Seven slots let [HomeExpertScreen] inject Hilt-backed screens without the shell depending on Hilt:
 * [topicsRoot], [dashboardRoot], [bookingsRoot] and [chatsRoot] render the Topics, Dashboard, Bookings and Chats
 * tab roots (defaults:
 * the placeholders, so Robolectric can render the shell with no Hilt graph), [profileRoot] renders the Profile hub
 * (default: the stateless hub with the real navigation actions; the shell hands it the debug affordances to show
 * below the version footer) and [detailEntries] registers extra
 * routes pushed on top of a tab, such as the Topic Editor and Availability. [pendingChatThread] /
 * [onConsumeChatThread] are the chat hand-off: when a thread key is pending the shell consumes it and opens it on the
 * Chats tab ([Navigator.openChatThread]). [pendingShellRoute] / [onConsumeShellRoute] are the push route hand-off: a
 * resolved expert [ShellRoute] is consumed only by this shell and inserted with [Navigator.openRouted].
 */
@Composable
fun HomeExpertShell(
    avatarUrl: String?,
    showDebugAffordances: Boolean,
    onSwitchMode: (AppMode) -> Unit,
    onLogout: () -> Unit,
    onDebugForceRefresh: () -> Unit,
    debugRefreshStatus: String?,
    modifier: Modifier = Modifier,
    dashboardRoot: @Composable (navigator: Navigator) -> Unit = { navigator ->
        ExpertTabRoot(
            key = DashboardRoot,
            actions = { TopBarAvatarButton(avatarUrl, onClick = { navigator.navigate(ModeSwitchKey) }) },
        )
    },
    bookingsRoot: @Composable (navigator: Navigator) -> Unit = { ExpertTabRoot(ExpertBookingsRoot) },
    topicsRoot: @Composable (navigator: Navigator) -> Unit = { ExpertTabRoot(TopicsRoot) },
    chatsRoot: @Composable (navigator: Navigator) -> Unit = { ExpertTabRoot(ExpertChatsRoot) },
    profileRoot: @Composable (navigator: Navigator, debugContent: @Composable () -> Unit) -> Unit =
        { navigator, debugContent ->
            // The no-op "View My Public Profile" exists only for the Hilt-free shell tests; production passes the
            // Hilt hub, which builds the self-view key from the signed-in user.
            ExpertProfileHomeContent(
                card = ProfileHubCard(name = null, email = null, avatarUrl = null),
                versionLabel = "",
                actions = expertProfileHubActions(navigator, onSwitchMode, onLogout),
                onViewPublicProfile = {},
                debugContent = debugContent,
            )
        },
    detailEntries: EntryProviderScope<NavKey>.(navigator: Navigator) -> Unit = {},
    pendingChatThread: ChatThreadKey? = null,
    onConsumeChatThread: () -> ChatThreadKey? = { null },
    pendingShellRoute: ShellRoute? = null,
    onConsumeShellRoute: () -> ShellRoute? = { null },
) {
    val state = rememberNavigationState(expertStartTab, expertTabs.map { it.key }.toSet())
    val navigator = remember(state) { Navigator(state) }
    val debugContent: @Composable () -> Unit = {
        if (showDebugAffordances) {
            ExpertProfileDebugAffordances(
                onOpenGallery = { navigator.navigate(DesignSystemGalleryKey) },
                onDebugForceRefresh = onDebugForceRefresh,
                debugRefreshStatus = debugRefreshStatus,
            )
        }
    }

    // A Message tap, or the in-call Chat control, posts a thread. Consume the slot so it applies exactly once.
    LaunchedEffect(pendingChatThread) {
        if (pendingChatThread != null) {
            onConsumeChatThread()?.let { navigator.openChatThread(ExpertChatsRoot, it) }
        }
    }

    // A resolved push route. Only the expert shell consumes an expert route: the outgoing shell of a mode switch is
    // still composed for the cross-fade and must leave a client route alone.
    LaunchedEffect(pendingShellRoute) {
        if (pendingShellRoute?.mode == AppMode.Expert) {
            onConsumeShellRoute()?.let { route ->
                val routable = route.tab in state.backStacks && route.keys.isNotEmpty()
                if (routable) navigator.openRouted(route.tab, route.keys)
            }
        }
    }

    HomeTabScaffold(
        tabs = expertTabs,
        navigationState = state,
        navigator = navigator,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<DashboardRoot> { dashboardRoot(navigator) }
                entry<ExpertBookingsRoot> { bookingsRoot(navigator) }
                entry<TopicsRoot> { topicsRoot(navigator) }
                entry<ExpertChatsRoot> { chatsRoot(navigator) }
                entry<ExpertProfileRoot> { profileRoot(navigator, debugContent) }
                entry<ModeSwitchKey>(metadata = BottomBar.hidden()) {
                    ModeSwitchScreen(
                        currentMode = AppMode.Expert,
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

/** Maps the expert hub's callbacks to destinations (PROF-01, D-02); shared by the Hilt hub and the shell default. */
internal fun expertProfileHubActions(
    navigator: Navigator,
    onSwitchMode: (AppMode) -> Unit,
    onLogout: () -> Unit,
): ExpertProfileHubActions =
    ExpertProfileHubActions(
        onEditProfile = { navigator.navigate(ExpertProfileSettingsKey(SettingsOrigin.Profile)) },
        onSwitchToClient = { onSwitchMode(AppMode.Client) },
        onOpenExpertise = { navigator.navigate(TopicsRoot) },
        onOpenAvailability = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Profile)) },
        onOpenPayouts = { navigator.navigate(PayoutsKey(SettingsOrigin.Profile)) },
        onOpenTaxProfile = { navigator.navigate(TaxProfileKey(SettingsOrigin.Profile)) },
        onOpenSecurity = { navigator.navigate(AccountSecurityKey) },
        onOpenHelp = { navigator.navigate(HelpKey) },
        onLogOut = onLogout,
    )

/**
 * The expert shell's own Expert Profile route: the "View My Public Profile" self-view and a chat-pushed peer profile
 * (RESEARCH Pitfall 8). Book Session is never shown in Expert mode (no booking route is registered here, and an
 * expert cannot book themselves), so the screen is told not to show it.
 */
internal fun EntryProviderScope<NavKey>.expertProfileEntry(navigator: Navigator) {
    entry<ExpertProfileKey>(metadata = BottomBar.hidden()) { key ->
        ExpertProfileScreen(
            viewModel =
                hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                ),
            onBack = navigator::goBack,
            onBook = {},
            isSelfView = key.isSelfView,
            showBookSession = false,
        )
    }
}

/** Debug-only rows below the hub's version footer; the shell renders them only when the debug gate is on. */
@Composable
private fun ExpertProfileDebugAffordances(
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
internal fun ExpertTabRoot(
    key: NavKey,
    actions: @Composable RowScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val tab = expertTabs.first { it.key == key }
    PlaceholderTabContent(
        title = stringResource(tab.labelRes),
        icon = tab.icon,
        actions = actions,
        footer = footer,
    )
}
