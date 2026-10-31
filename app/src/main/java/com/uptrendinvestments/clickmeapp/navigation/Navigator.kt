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
     * (explorePath.reset, bookingsPath.reset, selectedTab = .bookings) and `returnToHome` (explorePath.reset,
     * selectedTab = .explore); see the client callers `showMyBookings` and `showExplore` in
     * ClientBookingManagementEntries for the exact sets. Both also reset the Search stack (Phase 16.1, a deliberate
     * deviation from iOS). When [tab] is already selected this animates as a pop, since only its stack changes.
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

    /**
     * Select [tab] (the Chats tab) and show [key] (a chat thread) on its stack (D-13: every Message tap switches to
     * Chats and pushes the thread, from any tab or from the in-call control).
     *
     * A deliberate tightening of iOS `chatsPath.push`: when [key] is not already the top of [tab]'s stack, the stack
     * is popped to its root first, so Back from a thread always lands on the Conversations root and a second
     * hand-off replaces the open thread instead of stacking on it (UI-SPEC Navigation, RESEARCH A5). When [key] is
     * already on top, only the tab is selected. The originating tab keeps its own back stack.
     *
     * Unlike [navigateInTab] it never leaves stale entries on the target stack.
     */
    fun openChatThread(
        tab: NavKey,
        key: NavKey,
    ) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        val stack = state.backStacks.getValue(tab)
        if (stack.lastOrNull() != key) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            stack.add(key)
        }
        state.lastTransition = if (tab == state.topLevelRoute) NavTransition.Push else NavTransition.TabSwitch
        state.topLevelRoute = tab
    }

    /**
     * Inserts a push destination: the key chain [keys] (the last key is the screen the user lands on) on [tab]'s stack,
     * then selects [tab]. The Phase 13 push entry point (PUSH-04; RESEARCH Pattern 3, Pitfall 2).
     *
     * The stacks in use afterwards are the start stack and [tab]'s stack, and `toDecoratedEntries` flattens both, so
     * an equal key in two of them would crash. The rules, in order:
     * 1. **Dedupe the destination.** When the last key is already in one of those stacks, that stack is cut back to it
     *    and its tab is selected (`Pop` when it is the selected tab, else `TabSwitch`). Nothing is pushed.
     * 2. **Other tab.** Otherwise, when [tab] is not selected, its stack is popped to the root and the chain is added
     *    in order (`TabSwitch`). An earlier chain key that is already in the other stack in use is dropped: it was
     *    only a Back stop.
     * 3. **Selected tab.** When [tab] is selected, the stack is cut before the first entry equal to any chain key, so a
     *    repeated request chain is not stacked twice, and the chain goes on top (`Push`), so a row tap from the
     *    Notifications list keeps Back to the list.
     *
     * Back from the destination then follows [goBack]: pop, and from a non-start tab root select the start tab. Both
     * arguments are validated before anything is mutated.
     *
     * @throws IllegalArgumentException if [tab] is not a tab of this shell or [keys] is empty.
     */
    fun openRouted(
        tab: NavKey,
        keys: List<NavKey>,
    ) {
        require(tab in state.backStacks) { "Unknown tab: $tab" }
        require(keys.isNotEmpty()) { "Empty route chain" }
        val inUseAfter = listOf(state.startRoute, tab).distinct()
        val last = keys.last()

        for (stackTab in inUseAfter) {
            val stack = state.backStacks[stackTab] ?: continue
            val index = stack.indexOf(last)
            if (index >= 0) {
                while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
                state.lastTransition =
                    if (stackTab == state.topLevelRoute) NavTransition.Pop else NavTransition.TabSwitch
                state.topLevelRoute = stackTab
                return
            }
        }

        val target = state.backStacks.getValue(tab)
        val inOtherStacks = inUseAfter.filter { it != tab }.flatMap { state.backStacks[it].orEmpty() }.toSet()
        val chain = keys.dropLast(1).filter { it !in inOtherStacks } + last

        if (tab != state.topLevelRoute) {
            while (target.size > 1) target.removeAt(target.lastIndex)
            state.lastTransition = NavTransition.TabSwitch
        } else {
            val cut = (1 until target.size).firstOrNull { target[it] in chain }
            if (cut != null) {
                while (target.size > cut) target.removeAt(target.lastIndex)
            }
            state.lastTransition = NavTransition.Push
        }
        target.addAll(chain)
        state.topLevelRoute = tab
    }

    /**
     * Shows a screen that is already open, whichever tab holds it: the first stack (selected tab first, then the
     * others in tab order) with an entry accepted by [matches] is cut back to its topmost such entry and its tab is
     * selected. Nothing is pushed. Returns false, leaving the state untouched, when no stack holds a match.
     *
     * [openRouted] de-duplicates by key equality, so it cannot see a screen whose key differs only in a hint such as
     * the origin tab (Payouts opened from the Profile hub, then a payout push for the Dashboard). Calling this first
     * keeps one instance of such a screen alive.
     */
    fun openExisting(matches: (NavKey) -> Boolean): Boolean {
        val tabs = listOf(state.topLevelRoute) + state.backStacks.keys.filter { it != state.topLevelRoute }
        for (tab in tabs) {
            val stack = state.backStacks[tab] ?: continue
            val index = stack.indexOfLast(matches)
            if (index < 0) continue
            while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
            state.lastTransition = if (tab == state.topLevelRoute) NavTransition.Pop else NavTransition.TabSwitch
            state.topLevelRoute = tab
            return true
        }
        return false
    }

    private fun currentStack() =
        state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
}
