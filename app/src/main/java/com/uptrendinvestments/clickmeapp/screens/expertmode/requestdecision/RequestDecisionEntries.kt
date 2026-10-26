package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the Request Decision route in the expert shell.
 *
 * Plan 11-06 adds `entry<RequestDecisionKey>(metadata = BottomBar.hidden())` here, and plan 11-12 extends it. The
 * body is empty so the shell compiles before the feature exists.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.requestDecisionEntries(navigator: Navigator) {}
