package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelBookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsKey

/** Pushes Upcoming Booking Details for [bookingId] on the selected tab's stack (Explore card or Bookings card). */
fun Navigator.openUpcomingBooking(bookingId: String) = navigate(UpcomingBookingKey(bookingId))

/** Pushes Booking Summary for [bookingId] on the selected tab's stack (D-12, a Past card). */
fun Navigator.openBookingSummary(bookingId: String) = navigate(BookingSummaryKey(bookingId))

/**
 * Mirrors iOS `showMyBookings`: resets the Explore and Bookings stacks, then selects Bookings. Both stacks are
 * cleared so no details or cancel screen of a cancelled booking stays reachable by Back, whichever tab the flow
 * started from (D-07, RESEARCH Pattern 3, Pitfall 10).
 */
fun Navigator.showMyBookings() = resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot))

/** Mirrors iOS `returnToHome`: resets Explore to its root, then selects Explore (D-11, "Explore Experts"). */
fun Navigator.showExplore() = resetAndSelect(ExploreRoot, setOf(ExploreRoot))

/**
 * Registers the booking-management routes ([UpcomingBookingKey], [CancelBookingKey], [BookingSummaryKey]) in the
 * client shell, all with the bottom bar hidden. Hilt-free: the Hilt-backed screens arrive through [upcoming],
 * [cancel] and [summary], so HomeShellsTest drives the real metadata and navigation callbacks with fake content.
 *
 * - D-07: [cancel]'s `onCancelled` calls [showMyBookings]. The cancel ViewModel has already posted the tab and
 *   notice to `BookingsLaunchRequest`, so the screen only resets both stacks and selects Bookings.
 * - D-02: `onOpenPaymentMethods` (from the reschedule card) pushes [PaymentMethodsKey] on top of the booking
 *   screen; Back pops to it and it reloads quietly. [PaymentMethodsKey] stays registered once by
 *   [clientPaymentMethodsEntry], so it is not registered here.
 * - D-12: [upcoming]'s `onCancel` opens the cancel screen for the same booking.
 * - D-13: [summary]'s `onBookAgain` opens Make a Booking in the same stack.
 */
fun EntryProviderScope<NavKey>.clientBookingManagementEntries(
    navigator: Navigator,
    upcoming: @Composable (
        key: UpcomingBookingKey,
        onBack: () -> Unit,
        onCancel: () -> Unit,
        onOpenPaymentMethods: () -> Unit,
    ) -> Unit,
    cancel: @Composable (
        key: CancelBookingKey,
        onBack: () -> Unit,
        onCancelled: () -> Unit,
    ) -> Unit,
    summary: @Composable (
        key: BookingSummaryKey,
        onBack: () -> Unit,
        onBookAgain: (BookingKey) -> Unit,
        onOpenPaymentMethods: () -> Unit,
    ) -> Unit,
) {
    entry<UpcomingBookingKey>(metadata = BottomBar.hidden()) { key ->
        upcoming(
            key,
            navigator::goBack,
            { navigator.navigate(CancelBookingKey(key.bookingId)) },
            { navigator.navigate(PaymentMethodsKey) },
        )
    }
    entry<CancelBookingKey>(metadata = BottomBar.hidden()) { key ->
        cancel(key, navigator::goBack, navigator::showMyBookings)
    }
    entry<BookingSummaryKey>(metadata = BottomBar.hidden()) { key ->
        summary(
            key,
            navigator::goBack,
            { navigator.navigate(it) },
            { navigator.navigate(PaymentMethodsKey) },
        )
    }
}
