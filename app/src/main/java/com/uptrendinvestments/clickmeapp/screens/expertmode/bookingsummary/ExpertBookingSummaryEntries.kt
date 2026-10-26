package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the expert Booking Summary route in the expert shell.
 *
 * Plan 11-08 adds `entry<ExpertBookingSummaryKey>(metadata = BottomBar.hidden())` here, and plan 11-13 extends it.
 * The body is empty so the shell compiles before the feature exists.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.expertBookingSummaryEntries(navigator: Navigator) {}
