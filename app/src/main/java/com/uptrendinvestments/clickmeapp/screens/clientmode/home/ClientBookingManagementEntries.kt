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
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsKey

/** Pushes Upcoming Booking Details for [bookingId] on the selected tab's stack (Explore card or Bookings card). */
fun Navigator.openUpcomingBooking(bookingId: String) = navigate(UpcomingBookingKey(bookingId))

/** Pushes Booking Summary for [bookingId] on the selected tab's stack (D-12, a Past card). */
fun Navigator.openBookingSummary(bookingId: String) = navigate(BookingSummaryKey(bookingId))

/**
 * Mirrors iOS `showMyBookings`: resets the Explore and Bookings stacks, then selects Bookings. Both stacks are
 * cleared so no details or cancel screen of a cancelled booking stays reachable by Back, whichever tab the flow
 * started from (D-07, RESEARCH Pattern 3, Pitfall 10).
 *
 * Also resets the Search stack, a deliberate Android deviation (user decision 2026-10-30): iOS leaves `searchPath`
 * alone, which would park Request Sent (bottom bar hidden) or an Expert Profile on the Search stack after a booking
 * started from Search. [SearchRoot] itself is kept, so the Search ViewModel and its query survive (D-02).
 *
 * The Profile stack is deliberately not in this set: Saved Experts' "View My History" calls this function and must
 * leave Saved Experts open (RESEARCH A3, runbook row D1.FV4). A booking that ran on the Profile stack is reset by
 * [viewBookingsFromRequestSent], a second deliberate Android deviation (Phase 16.3 CR-01).
 */
fun Navigator.showMyBookings() = resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, SearchRoot, ClientBookingsRoot))

/**
 * Mirrors iOS `returnToHome`: resets Explore to its root, then selects Explore (D-11, "Explore Experts").
 *
 * Also resets the Search stack, the same deliberate Android deviation as [showMyBookings] (user decision
 * 2026-10-30): a booking started from Search must not leave Request Sent parked there. [SearchRoot] is kept (D-02).
 *
 * The Profile stack is deliberately not in this set: Saved Experts' "Explore Experts" calls this function and must
 * leave Saved Experts open (RESEARCH A3, runbook row D1.FV4). A booking that ran on the Profile stack is reset by
 * [backToHomeFromRequestSent], a second deliberate Android deviation (Phase 16.3 CR-01).
 */
fun Navigator.showExplore() = resetAndSelect(ExploreRoot, setOf(ExploreRoot, SearchRoot))

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
 * - Phase 16.2 RS-C1: [upcoming]'s `onReschedule` opens the picker with opener `UpcomingDetails` and the details key's
 *   origin, so a chat-opened booking stays a distinct key.
 * - D-13: [summary]'s `onBookAgain` opens Make a Booking in the same stack.
 * - D-18: [summary]'s `onLeaveReview` pushes Write Review on the same stack as the summary; Back returns to it.
 * - Phase 16.2 D-09: [reschedule] (the picker) pops to its opener.
 * - Phase 16.2 D-06/D-08: Add a Payment Method pushes Payment Methods above the picker and nothing is retried on return.
 */
fun EntryProviderScope<NavKey>.clientBookingManagementEntries(
    navigator: Navigator,
    upcoming: @Composable (
        key: UpcomingBookingKey,
        onBack: () -> Unit,
        onCancel: () -> Unit,
        onOpenPaymentMethods: () -> Unit,
        onReschedule: () -> Unit,
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
        onLeaveReview: (WriteReviewKey) -> Unit,
    ) -> Unit,
    reschedule: @Composable (
        key: RescheduleKey,
        onBack: () -> Unit,
        onOpenPaymentMethods: () -> Unit,
    ) -> Unit,
) {
    entry<RescheduleKey>(metadata = BottomBar.hidden()) { key ->
        reschedule(key, navigator::goBack) { navigator.navigate(PaymentMethodsKey) }
    }
    entry<UpcomingBookingKey>(metadata = BottomBar.hidden()) { key ->
        upcoming(
            key,
            navigator::goBack,
            { navigator.navigate(CancelBookingKey(key.bookingId)) },
            { navigator.navigate(PaymentMethodsKey) },
            { navigator.navigate(RescheduleKey(key.bookingId, RescheduleOpener.UpcomingDetails, key.origin)) },
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
            { navigator.navigate(it) },
        )
    }
}
