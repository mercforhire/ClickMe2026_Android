package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsKey
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsOrigin
import com.uptrendinvestments.clickmeapp.testing.FakeClientBookingProbe
import com.uptrendinvestments.clickmeapp.testing.FakePendingRequestLookup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushDestinationResolverTest {
    private val probe = FakeClientBookingProbe()
    private val requests = FakePendingRequestLookup()
    private val resolver = PushDestinationResolver(probe, requests)

    private fun context(
        mode: AppMode = AppMode.Client,
        hasExpertRole: Boolean = true,
        callActive: Boolean = false,
    ) = PushRoutingContext(mode, hasExpertRole, callActive)

    private fun clientRoute(vararg keys: androidx.navigation3.runtime.NavKey) =
        ResolvedPush(AppMode.Client, PushRoute.Shell(ShellRoute(AppMode.Client, ClientBookingsRoot, keys.toList())))

    @Test
    fun upcomingStatusOpensUpcomingBooking() =
        runTest {
            listOf("confirmed", "pending_approval", "pending_reschedule", "in_progress").forEach { status ->
                probe.result = { ClientBookingProbeResult.Client(status) }
                val resolved =
                    resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Client), context())
                assertEquals(status, clientRoute(UpcomingBookingKey("B1")), resolved)
            }
        }

    @Test
    fun pastStatusOpensBookingSummary() =
        runTest {
            listOf("completed", "cancelled", "declined", "missed", "expired", "some_future_status").forEach { status ->
                probe.result = { ClientBookingProbeResult.Client(status) }
                val resolved =
                    resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Client), context())
                assertEquals(status, clientRoute(BookingSummaryKey("B1")), resolved)
            }
        }

    @Test
    fun notificationsListOpensCurrentModeList() =
        runTest {
            val client = resolver.resolve(PushDestination.NotificationsList, context(AppMode.Client))
            assertEquals(
                ResolvedPush(
                    AppMode.Client,
                    PushRoute.Shell(
                        ShellRoute(
                            AppMode.Client,
                            ExploreRoot,
                            listOf(NotificationsKey(NotificationsOrigin.ClientExplore)),
                        ),
                    ),
                ),
                client,
            )
            val expert = resolver.resolve(PushDestination.NotificationsList, context(AppMode.Expert))
            assertEquals(
                ResolvedPush(
                    AppMode.Expert,
                    PushRoute.Shell(
                        ShellRoute(
                            AppMode.Expert,
                            DashboardRoot,
                            listOf(NotificationsKey(NotificationsOrigin.ExpertDashboard)),
                        ),
                    ),
                ),
                expert,
            )
        }
}
