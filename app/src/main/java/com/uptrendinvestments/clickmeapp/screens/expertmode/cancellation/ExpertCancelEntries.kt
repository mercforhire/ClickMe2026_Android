package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the expert Cancel Booking route in the expert shell.
 *
 * Plan 11-09 adds `entry<ExpertCancelKey>(metadata = BottomBar.hidden())` here. The body is empty so the shell
 * compiles before the feature exists.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.expertCancelEntries(navigator: Navigator) {}
