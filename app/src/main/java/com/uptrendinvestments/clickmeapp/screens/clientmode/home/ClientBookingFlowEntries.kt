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
 * - D-13: `onViewBookings` and `onBackToHome` go through [viewBookingsFromRequestSent] and
 *   [backToHomeFromRequestSent], which mirror iOS `showMyBookings` (reset the Explore and Bookings paths, then select
 *   Bookings) and `returnToHome` (reset Explore to its root, then select Explore) and also reset the Profile stack
 *   when the flow ran there. `onBackToHome` lands on Explore even if a future entry point starts the flow from
 *   another tab.
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

/**
 * [showMyBookings] plus an Android-only reset of the Profile stack when this booking's Request Sent is on it
 * (Phase 16.3 CR-01).
 *
 * iOS `showMyBookings` and `returnToHome` leave `profilePath` alone, which would park Request Sent (bottom bar hidden)
 * on the Profile tab after a booking started from Saved Experts, the same defect the Search reset removes.
 * [ClientProfileRoot] itself is kept.
 *
 * Deliberate trade-off: after a finished booking the Profile tab opens on the hub, so Saved Experts is gone, while
 * Saved Experts' own "Explore Experts" and "View My History" keep it open (runbook row D1.FV4). That is why this reset
 * lives here and not in the shared sets of [showMyBookings] and [showExplore].
 *
 * A booking on another stack is unaffected, and an Expert Profile left open on the Profile tab survives it. The Chats
 * stack on both exits and the Bookings stack on "Back to home" still keep their Request Sent, a pre-existing
 * behavior this change does not touch.
 */
internal fun Navigator.viewBookingsFromRequestSent(key: RequestSentKey) {
    resetProfileStackHolding(key)
    showMyBookings()
}

/** [showExplore] after a finished booking, also returning the Profile tab to the hub; see [viewBookingsFromRequestSent]. */
internal fun Navigator.backToHomeFromRequestSent(key: RequestSentKey) {
    resetProfileStackHolding(key)
    showExplore()
}

/** Checks stack membership rather than the selected tab, so it describes the stack that actually holds the screen. */
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
