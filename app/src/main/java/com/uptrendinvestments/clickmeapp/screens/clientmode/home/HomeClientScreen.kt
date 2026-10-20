package com.uptrendinvestments.clickmeapp.screens.clientmode.home

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
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientStartTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientTabs
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryKey
import com.uptrendinvestments.clickmeapp.screens.debug.DesignSystemGalleryScreen
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Hilt-aware entry for the client shell: supplies the avatar URL and the debug gate. */
@Composable
fun HomeClientScreen(
    onDebugSwitchScaffold: () -> Unit,
    viewModel: HomeClientViewModel = hiltViewModel(),
) {
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    HomeClientShell(
        avatarUrl = avatarUrl,
        showDebugAffordances = BuildConfig.DEBUG,
        onDebugSwitchScaffold = onDebugSwitchScaffold,
        // Phase 1: the avatar does nothing yet; Phase 4 wires it to the mode switch.
        onAvatarClick = {},
    )
}

/**
 * The client 5-tab shell. Stateless apart from navigation state, with no Hilt or BuildConfig
 * access, so it renders under Robolectric.
 */
@Composable
fun HomeClientShell(
    avatarUrl: String?,
    showDebugAffordances: Boolean,
    onDebugSwitchScaffold: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberNavigationState(clientStartTab, clientTabs.map { it.key }.toSet())
    val navigator = remember(state) { Navigator(state) }

    HomeTabScaffold(
        tabs = clientTabs,
        navigationState = state,
        navigator = navigator,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<ExploreRoot> {
                    ClientTabRoot(
                        key = ExploreRoot,
                        actions = { TopBarAvatarButton(avatarUrl, onAvatarClick) },
                    )
                }
                entry<SearchRoot> { ClientTabRoot(SearchRoot) }
                entry<ClientBookingsRoot> { ClientTabRoot(ClientBookingsRoot) }
                entry<ClientChatsRoot> { ClientTabRoot(ClientChatsRoot) }
                entry<ClientProfileRoot> {
                    ClientTabRoot(
                        key = ClientProfileRoot,
                        footer = {
                            if (showDebugAffordances) {
                                OutlinedButton(onClick = onDebugSwitchScaffold) {
                                    Text(stringResource(R.string.debug_switch_to_expert))
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
