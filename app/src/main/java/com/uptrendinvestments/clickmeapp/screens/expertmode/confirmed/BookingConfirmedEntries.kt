package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the Booking Confirmed route in the expert shell.
 *
 * Plan 11-07 adds `entry<BookingConfirmedKey>(metadata = BottomBar.hidden())` here. The body is empty so the shell
 * compiles before the feature exists.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.bookingConfirmedEntries(navigator: Navigator) {}
