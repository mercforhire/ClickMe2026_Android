package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the Incoming Requests route in the expert shell.
 *
 * Plan 11-05 adds `entry<IncomingRequestsKey>(metadata = BottomBar.hidden())` here. The body is empty so the shell
 * compiles before the feature exists.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.incomingRequestsEntries(navigator: Navigator) {}
