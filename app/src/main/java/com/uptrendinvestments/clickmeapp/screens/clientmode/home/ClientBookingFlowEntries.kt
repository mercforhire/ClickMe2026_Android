package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.RequestSentKey
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsKey

/**
 * Registers the booking flow routes ([BookingKey] and [RequestSentKey]) in the client shell, both with the
 * bottom bar hidden. Hilt-free: the Hilt-backed screens arrive through [booking] and [requestSent], so
 * HomeShellsTest drives the real metadata and navigation callbacks with fake content.
 *
 * - D-12: [booking]'s `onBooked` replaces Make a Booking with Request Sent (`replaceTop`), so Back from Request
 *   Sent lands on Expert Profile and the filled form cannot be re-submitted.
 * - D-08: `onOpenPaymentMethods` pushes [PaymentMethodsKey] on top of the booking form; Back pops to the form, which
 *   reloads its cards (D-09).
 * - D-13: `onViewBookings` mirrors iOS `showMyBookings` (reset the Explore and Bookings paths, then select
 *   Bookings); `onBackToHome` mirrors iOS `returnToHome` (reset Explore to its root, then select Explore), so it
 *   lands on Explore even if a future entry point starts the flow from another tab.
 *
 * The Bookings tab is My Bookings since Phase 8 (see [clientBookingManagementEntries]), so "View My Bookings" lands
 * on the real list.
 */
fun EntryProviderScope<NavKey>.clientBookingFlowEntries(
    navigator: Navigator,
    booking: @Composable (
        key: BookingKey,
        onBack: () -> Unit,
        onBooked: (bookingId: String) -> Unit,
        onOpenPaymentMethods: () -> Unit,
    ) -> Unit,
    requestSent: @Composable (
        key: RequestSentKey,
        onBack: () -> Unit,
        onViewBookings: () -> Unit,
        onBackToHome: () -> Unit,
    ) -> Unit,
) {
    entry<BookingKey>(metadata = BottomBar.hidden()) { key ->
        booking(
            key,
            navigator::goBack,
            { id -> navigator.replaceTop(RequestSentKey(id)) },
            { navigator.navigate(PaymentMethodsKey) },
        )
    }
    entry<RequestSentKey>(metadata = BottomBar.hidden()) { key ->
        requestSent(
            key,
            navigator::goBack,
            { navigator.viewBookingsFromRequestSent(key) },
            { navigator.backToHomeFromRequestSent(key) },
        )
    }
}

/** [showMyBookings] after a finished booking, also returning the Profile tab to the hub (see Task 3 KDoc). */
internal fun Navigator.viewBookingsFromRequestSent(key: RequestSentKey) {
    resetProfileStackHolding(key)
    showMyBookings()
}

/** [showExplore] after a finished booking, also returning the Profile tab to the hub. */
internal fun Navigator.backToHomeFromRequestSent(key: RequestSentKey) {
    resetProfileStackHolding(key)
    showExplore()
}

private fun Navigator.resetProfileStackHolding(key: RequestSentKey) {
    if (key in state.backStacks[ClientProfileRoot].orEmpty()) reselect(ClientProfileRoot)
}

/**
 * Registers [PaymentMethodsKey] once for the client shell, with the bottom bar hidden. One destination serves both
 * entry points (D-04): it is pushed on whichever tab is selected, the Explore stack from the booking gate and the
 * Profile stack from the Profile row. Hilt-free so HomeShellsTest can drive it with fake content.
 */
fun EntryProviderScope<NavKey>.clientPaymentMethodsEntry(
    navigator: Navigator,
    paymentMethods: @Composable (onBack: () -> Unit) -> Unit,
) {
    entry<PaymentMethodsKey>(metadata = BottomBar.hidden()) { paymentMethods(navigator::goBack) }
}
