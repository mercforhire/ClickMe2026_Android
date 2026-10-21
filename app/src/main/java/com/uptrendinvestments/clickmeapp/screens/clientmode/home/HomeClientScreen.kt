package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
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
import com.uptrendinvestments.clickmeapp.screens.onboarding.OnboardingContent
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchKey
import com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.ModeSwitchScreen
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Hilt-aware entry for the client shell: supplies the avatar URL, session actions and the debug gate. */
@Composable
fun HomeClientScreen(
    onSwitchMode: (AppMode) -> Unit,
    viewModel: HomeClientViewModel = hiltViewModel(),
) {
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
                        actions = { TopBarAvatarButton(avatarUrl, onClick = { navigator.navigate(ModeSwitchKey) }) },
                    )
                }
                entry<SearchRoot> { ClientTabRoot(SearchRoot) }
                entry<ClientBookingsRoot> { ClientTabRoot(ClientBookingsRoot) }
                entry<ClientChatsRoot> { ClientTabRoot(ClientChatsRoot) }
                entry<ClientProfileRoot> {
                    ClientTabRoot(
                        key = ClientProfileRoot,
                        footer = {
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
                        currentMode = AppMode.Client,
                        onSwitchMode = onSwitchMode,
                        onBack = navigator::goBack,
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
