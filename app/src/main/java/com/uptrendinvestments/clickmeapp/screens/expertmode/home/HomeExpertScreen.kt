package com.uptrendinvestments.clickmeapp.screens.expertmode.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PlaceholderTabContent
import com.uptrendinvestments.clickmeapp.components.ProfileMenuRow
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.HomeTabScaffold
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.rememberNavigationState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.chatEntries
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
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
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.requestDecisionEntries
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorViewModel
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRoute
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
) {
    val pendingChatThread by chatLaunch.pendingThread.collectAsStateWithLifecycle()
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
        },
        pendingChatThread = pendingChatThread,
        onConsumeChatThread = chatLaunch::consumeThread,
    )
}

/**
 * The expert 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 *
 * Five slots let [HomeExpertScreen] inject Hilt-backed screens without the shell depending on Hilt:
 * [topicsRoot], [dashboardRoot] and [bookingsRoot] render the Topics, Dashboard and Bookings tab roots (defaults:
 * the placeholders, so Robolectric can render the shell with no Hilt graph) and [detailEntries] registers extra
 * routes pushed on top of a tab, such as the Topic Editor and Availability. [pendingChatThread] /
 * [onConsumeChatThread] are the chat hand-off: when a thread key is pending the shell consumes it and opens it on the
 * Chats tab ([Navigator.openChatThread]).
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
    detailEntries: EntryProviderScope<NavKey>.(navigator: Navigator) -> Unit = {},
    pendingChatThread: ChatThreadKey? = null,
    onConsumeChatThread: () -> ChatThreadKey? = { null },
) {
    val state = rememberNavigationState(expertStartTab, expertTabs.map { it.key }.toSet())
    val navigator = remember(state) { Navigator(state) }

    // A Message tap, or the in-call Chat control, posts a thread. Consume the slot so it applies exactly once.
    LaunchedEffect(pendingChatThread) {
        if (pendingChatThread != null) {
            onConsumeChatThread()?.let { navigator.openChatThread(ExpertChatsRoot, it) }
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
                entry<ExpertChatsRoot> { ExpertTabRoot(ExpertChatsRoot) }
                entry<ExpertProfileRoot> {
                    ExpertTabRoot(
                        key = ExpertProfileRoot,
                        footer = {
                            // Throwaway entry point, replaced by the Phase 14 Profile hub (D-01).
                            ProfileMenuRow(
                                title = stringResource(R.string.availability_title),
                                icon = Icons.Outlined.CalendarMonth,
                                onClick = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Profile)) },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md)
                                        .testTag("profile_availability_row"),
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            OutlinedButton(onClick = onLogout) {
                                Text(stringResource(R.string.profile_log_out))
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            if (showDebugAffordances) {
                                OutlinedButton(onClick = { navigator.navigate(DesignSystemGalleryKey) }) {
                                    Text(stringResource(R.string.debug_design_system_gallery))
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                OutlinedButton(onClick = onDebugForceRefresh) {
                                    Text(stringResource(R.string.debug_force_token_refresh))
                                }
                                if (debugRefreshStatus != null) {
                                    Text(debugRefreshStatus)
                                }
                            }
                        },
                    )
                }
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
