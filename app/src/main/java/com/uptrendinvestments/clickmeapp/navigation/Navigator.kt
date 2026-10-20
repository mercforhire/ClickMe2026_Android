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

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward, back, tab selection) by updating [NavigationState].
 */
class Navigator(
    val state: NavigationState,
) {
    /**
     * Select a top level route, or push [route] on the current tab's stack.
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (route != state.topLevelRoute) {
                state.lastTransition = NavTransition.TabSwitch
                state.topLevelRoute = route
            }
        } else {
            state.lastTransition = NavTransition.Push
            currentStack().add(route)
        }
    }

    /**
     * Pop within the current tab. At a non-home tab root, select the home tab. At the home root
     * this is a no-op (NavDisplay then has a single entry, so system Back exits the app).
     */
    fun goBack() {
        val stack = currentStack()
        if (stack.size > 1) {
            state.lastTransition = NavTransition.Pop
            stack.removeAt(stack.lastIndex)
        } else if (state.topLevelRoute != state.startRoute) {
            state.lastTransition = NavTransition.TabSwitch
            state.topLevelRoute = state.startRoute
        }
    }

    /** Bottom bar click: re-tapping the selected tab pops it to root, otherwise switch tabs. */
    fun onTabSelected(tab: NavKey) {
        if (tab == state.topLevelRoute) reselect(tab) else navigate(tab)
    }

    /** Pop [tab]'s stack down to its root. */
    fun reselect(tab: NavKey) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        val stack = state.backStacks.getValue(tab)
        state.lastTransition = NavTransition.Pop
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /**
     * Select [tab] and push [route] on its stack. This is the shell-level cross-tab API from the
     * UI-SPEC (iOS "message" jumps to Chats), consumed by Phase 12 chat entry points and Phase 13
     * push routing.
     */
    fun navigateInTab(
        tab: NavKey,
        route: NavKey,
    ) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        state.backStacks.getValue(tab).add(route)
        state.lastTransition = NavTransition.TabSwitch
        state.topLevelRoute = tab
    }

    private fun currentStack() =
        state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
}
