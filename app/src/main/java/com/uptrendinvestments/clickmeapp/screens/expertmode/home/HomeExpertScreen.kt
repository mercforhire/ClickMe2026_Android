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
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryKey
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertStartTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertTabs
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicsScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorViewModel
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchScreen
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Hilt-aware entry for the expert shell: supplies the avatar URL, session actions and the debug gate. */
@Composable
fun HomeExpertScreen(
    onSwitchMode: (AppMode) -> Unit,
    viewModel: HomeExpertViewModel = hiltViewModel(),
) {
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val debugRefreshStatus by viewModel.debugRefreshStatus.collectAsStateWithLifecycle()
    HomeExpertShell(
        avatarUrl = avatarUrl,
        showDebugAffordances = BuildConfig.DEBUG,
        onSwitchMode = onSwitchMode,
        onLogout = viewModel::logout,
        onDebugForceRefresh = viewModel::debugForceRefresh,
        debugRefreshStatus = debugRefreshStatus,
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
        },
    )
}

/**
 * The expert 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 *
 * Two slots let [HomeExpertScreen] inject Hilt-backed screens without the shell depending on Hilt:
 * [topicsRoot] renders the Topics tab root (default: the placeholder) and [detailEntries] registers extra routes
 * pushed on top of a tab, such as the Topic Editor and Availability.
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
    topicsRoot: @Composable (navigator: Navigator) -> Unit = { ExpertTabRoot(TopicsRoot) },
    detailEntries: EntryProviderScope<NavKey>.(navigator: Navigator) -> Unit = {},
) {
    val state = rememberNavigationState(expertStartTab, expertTabs.map { it.key }.toSet())
    val navigator = remember(state) { Navigator(state) }

    HomeTabScaffold(
        tabs = expertTabs,
        navigationState = state,
        navigator = navigator,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<DashboardRoot> {
                    ExpertTabRoot(
                        key = DashboardRoot,
                        actions = { TopBarAvatarButton(avatarUrl, onClick = { navigator.navigate(ModeSwitchKey) }) },
                    )
                }
                entry<ExpertBookingsRoot> { ExpertTabRoot(ExpertBookingsRoot) }
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
private fun ExpertTabRoot(
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
