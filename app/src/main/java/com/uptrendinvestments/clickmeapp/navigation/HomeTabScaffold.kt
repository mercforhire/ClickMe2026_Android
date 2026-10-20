package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

/** NavEntry metadata that lets a route opt out of the bottom bar (UI-SPEC showsBottomBar flag). */
object BottomBar {
    const val HIDE_KEY: String = "clickme.bottomBar.hidden"

    fun hidden(): Map<String, Any> = mapOf(HIDE_KEY to true)
}

private fun fadeThrough(): ContentTransform =
    fadeIn(tween(Motion.TAB_FADE_IN_MS, delayMillis = Motion.TAB_FADE_OUT_MS)) togetherWith
        fadeOut(tween(Motion.TAB_FADE_OUT_MS))

private fun slide(forward: Boolean): ContentTransform {
    val spec = tween<Float>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    val offsetSpec = tween<IntOffset>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(offsetSpec) { it * direction } + fadeIn(spec)) togetherWith
        (slideOutHorizontally(offsetSpec) { -it * direction } + fadeOut(spec))
}

/**
 * Shared tab shell for the client and expert homes: a NavigationBar over a NavDisplay that renders
 * the per-tab back stacks of [navigationState]. Takes plain parameters only (no Hilt, no
 * BuildConfig) so it renders under Robolectric. Tab-root screens own their own TopAppBar.
 */
@Composable
fun HomeTabScaffold(
    tabs: List<TabDefinition>,
    navigationState: NavigationState,
    navigator: Navigator,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    val entries = navigationState.toDecoratedEntries(entryProvider)
    val showBottomBar = entries.lastOrNull()?.metadata?.get(BottomBar.HIDE_KEY) != true
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        containerColor = colors.surface,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = colors.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = tab.key == navigationState.topLevelRoute
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigator.onTabSelected(tab.key) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            alwaysShowLabel = true,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.primary,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.secondaryContainer,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant,
                                ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            entries = entries,
            onBack = { navigator.goBack() },
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            transitionSpec = {
                when (navigationState.lastTransition) {
                    NavTransition.TabSwitch -> fadeThrough()
                    else -> slide(forward = true)
                }
            },
            popTransitionSpec = {
                when (navigationState.lastTransition) {
                    NavTransition.TabSwitch -> fadeThrough()
                    else -> slide(forward = false)
                }
            },
            predictivePopTransitionSpec = {
                when (navigationState.lastTransition) {
                    NavTransition.TabSwitch -> fadeThrough()
                    else -> slide(forward = false)
                }
            },
        )
    }
}
