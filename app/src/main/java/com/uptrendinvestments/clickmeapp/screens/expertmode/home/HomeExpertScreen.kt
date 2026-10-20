package com.uptrendinvestments.clickmeapp.screens.expertmode.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PlaceholderTabContent
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.navigation.HomeTabScaffold
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.rememberNavigationState
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryKey
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertStartTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertTabs
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Hilt-aware entry for the expert shell: supplies the avatar URL and the debug gate. */
@Composable
fun HomeExpertScreen(
    onDebugSwitchScaffold: () -> Unit,
    viewModel: HomeExpertViewModel = hiltViewModel(),
) {
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    HomeExpertShell(
        avatarUrl = avatarUrl,
        showDebugAffordances = BuildConfig.DEBUG,
        onDebugSwitchScaffold = onDebugSwitchScaffold,
        // Phase 1: the avatar does nothing yet; Phase 4 wires it to the mode switch.
        onAvatarClick = {},
    )
}

/**
 * The expert 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 */
@Composable
fun HomeExpertShell(
    avatarUrl: String?,
    showDebugAffordances: Boolean,
    onDebugSwitchScaffold: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                        actions = { TopBarAvatarButton(avatarUrl, onAvatarClick) },
                    )
                }
                entry<ExpertBookingsRoot> { ExpertTabRoot(ExpertBookingsRoot) }
                entry<TopicsRoot> { ExpertTabRoot(TopicsRoot) }
                entry<ExpertChatsRoot> { ExpertTabRoot(ExpertChatsRoot) }
                entry<ExpertProfileRoot> {
                    ExpertTabRoot(
                        key = ExpertProfileRoot,
                        footer = {
                            if (showDebugAffordances) {
                                OutlinedButton(onClick = onDebugSwitchScaffold) {
                                    Text(stringResource(R.string.debug_switch_to_client))
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                OutlinedButton(onClick = { navigator.navigate(DesignSystemGalleryKey) }) {
                                    Text(stringResource(R.string.debug_design_system_gallery))
                                }
                            }
                        },
                    )
                }
                entry<DesignSystemGalleryKey> {
                    DesignSystemGalleryScreen(onBack = navigator::goBack)
                }
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
