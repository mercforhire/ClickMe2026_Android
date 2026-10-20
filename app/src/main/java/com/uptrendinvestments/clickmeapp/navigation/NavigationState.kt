/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Adapted from github.com/android/nav3-recipes (multiplestacks recipe) for ClickMe.

package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/** What kind of change the last navigation was; drives the NavDisplay transition spec. */
enum class NavTransition { Push, Pop, TabSwitch }

/**
 * Create a navigation state that persists config changes and process death.
 *
 * @param startRoute The top level route to start on. This should also be in [topLevelRoutes].
 * @param topLevelRoutes The top level routes (tabs) of the shell.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute =
        rememberSerializable(
            startRoute,
            topLevelRoutes,
            serializer = MutableStateSerializer(NavKeySerializer()),
        ) {
            mutableStateOf(startRoute)
        }

    // One back stack per top level route.
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation state. It does not modify its own state; use [Navigator].
 *
 * @param startRoute The start (home) route. The user exits the app through this route.
 * @param topLevelRoute The state object that backs the selected top level route.
 * @param backStacks The back stack of each top level route.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** The selected top level route. */
    var topLevelRoute: NavKey by topLevelRoute

    /** Kind of the last navigation. Transient (not saved); read by the transition specs. */
    var lastTransition: NavTransition by mutableStateOf(NavTransition.TabSwitch)

    /**
     * The top level routes currently shown. The start route is always first, so the user exits the
     * app through home ("exit through home"). At most one other tab is in use. The state of unused
     * stacks is still retained.
     */
    val stacksInUse: List<NavKey>
        get() =
            if (topLevelRoute == startRoute) {
                listOf(startRoute)
            } else {
                listOf(startRoute, topLevelRoute)
            }
}

/**
 * Convert the navigation state into [NavEntry]s decorated with a SaveableStateHolder and a
 * ViewModelStore, flattened over [NavigationState.stacksInUse].
 *
 * @param entryProvider Converts the keys in the back stacks to [NavEntry]s.
 */
@Composable
fun NavigationState.toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
    val decoratedEntries =
        backStacks.mapValues { (_, stack) ->
            val decorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }
    return stacksInUse.flatMap { decoratedEntries[it] ?: emptyList() }
}
