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
     *
     * A push of a route equal to the current top of the stack is ignored. Outgoing content still accepts
     * input during the push transition, so a quick double tap would otherwise stack two equal keys that
     * share one Nav3 `contentKey` (and so one ViewModel and saveable state) and need two Backs to leave.
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (route != state.topLevelRoute) {
                state.lastTransition = NavTransition.TabSwitch
                state.topLevelRoute = route
            }
        } else {
            val stack = currentStack()
            if (stack.lastOrNull() == route) return
            state.lastTransition = NavTransition.Push
            stack.add(route)
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

    /** Pop [tab]'s stack down to its root without changing the selected tab (bottom bar re-tap). */
    fun reselect(tab: NavKey) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        val stack = state.backStacks.getValue(tab)
        state.lastTransition = NavTransition.Pop
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /**
     * Replace the top entry of the current tab's stack with [route] in one change (booking to Request Sent,
     * D-12), so Back from [route] skips the replaced screen. The removed entry's ViewModel store is cleared
     * by the Nav3 decorator, which is intended (RESEARCH Pitfall 11).
     *
     * Unlike [navigate] this does not apply the "same as top is ignored" rule; replacing with an equal key
     * is a caller error that the root check below does not need to cover.
     *
     * @throws IllegalStateException if the current stack is only its tab root.
     */
    fun replaceTop(route: NavKey) {
        val stack = currentStack()
        check(stack.size > 1) { "Cannot replace a tab root" }
        state.lastTransition = NavTransition.Push
        stack.removeAt(stack.lastIndex)
        stack.add(route)
    }

    /**
     * Pop every tab in [reset] to its root, then select [tab] (D-13). Mirrors iOS `showMyBookings`
     * (explorePath.reset, bookingsPath.reset, selectedTab = .bookings; the caller passes
     * `setOf(ExploreRoot, ClientBookingsRoot)`) and `returnToHome` (explorePath.reset, selectedTab = .explore;
     * the caller passes `setOf(ExploreRoot)`). When [tab] is already selected this animates as a pop, since
     * only its stack changes.
     */
    fun resetAndSelect(
        tab: NavKey,
        reset: Collection<NavKey>,
    ) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        // Validate every key before mutating anything, so a bad call leaves the stacks untouched.
        val stacks = reset.map { r -> requireNotNull(state.backStacks[r]) { "Unknown tab: $r" } }
        for (stack in stacks) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        }
        state.lastTransition = if (tab == state.topLevelRoute) NavTransition.Pop else NavTransition.TabSwitch
        state.topLevelRoute = tab
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
