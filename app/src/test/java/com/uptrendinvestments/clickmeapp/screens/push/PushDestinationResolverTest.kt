package com.uptrendinvestments.clickmeapp.screens.push

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsKey
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsOrigin
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityKey
import com.uptrendinvestments.clickmeapp.testing.FakeClientBookingProbe
import com.uptrendinvestments.clickmeapp.testing.FakePendingRequestLookup
import kotlinx.coroutines.delay
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

    private fun clientRoute(vararg keys: NavKey) =
        ResolvedPush(AppMode.Client, PushRoute.Shell(ShellRoute(AppMode.Client, ClientBookingsRoot, keys.toList())))

    private fun expertRoute(vararg keys: NavKey) =
        ResolvedPush(AppMode.Expert, PushRoute.Shell(ShellRoute(AppMode.Expert, ExpertBookingsRoot, keys.toList())))

    private fun expertSummary(id: String = "B1") = ExpertBookingSummaryKey(id, ExpertBookingOrigin.Bookings)

    private fun clientList() =
        ResolvedPush(
            AppMode.Client,
            PushRoute.Shell(
                ShellRoute(AppMode.Client, ExploreRoot, listOf(NotificationsKey(NotificationsOrigin.ClientExplore))),
            ),
        )

    private fun expertList() =
        ResolvedPush(
            AppMode.Expert,
            PushRoute.Shell(
                ShellRoute(
                    AppMode.Expert,
                    DashboardRoot,
                    listOf(NotificationsKey(NotificationsOrigin.ExpertDashboard)),
                ),
            ),
        )

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

    @Test
    fun notClientProbeRoutesToExpertSummary() =
        runTest {
            probe.result = { ClientBookingProbeResult.NotClient }
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Either), context(AppMode.Expert))
            assertEquals(expertRoute(expertSummary()), resolved)
        }

    @Test
    fun expertAudienceSkipsTheProbe() =
        runTest {
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Expert), context(AppMode.Expert))
            assertEquals(expertRoute(expertSummary()), resolved)
            assertEquals(emptyList<String>(), probe.probed)
        }

    @Test
    fun unknownProbeClientAudienceKeepsClientSummary() =
        runTest {
            probe.result = { ClientBookingProbeResult.Unknown }
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Client), context(AppMode.Client))
            assertEquals(clientRoute(BookingSummaryKey("B1")), resolved)
        }

    @Test
    fun unknownProbeEitherRoutesInCurrentMode() =
        runTest {
            probe.result = { ClientBookingProbeResult.Unknown }
            val either = PushDestination.BookingDetail("B1", PushAudience.Either)
            assertEquals(clientRoute(BookingSummaryKey("B1")), resolver.resolve(either, context(AppMode.Client)))
            assertEquals(expertRoute(expertSummary()), resolver.resolve(either, context(AppMode.Expert)))
        }

    @Test
    fun slowProbeTimesOutAsUnknown() =
        runTest {
            probe.result = {
                delay(LOOKUP_TIMEOUT_MS + 1)
                ClientBookingProbeResult.Client("confirmed")
            }
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Client), context(AppMode.Client))
            assertEquals(clientRoute(BookingSummaryKey("B1")), resolved)
        }

    @Test
    fun leaveReviewClientOpensSummary() =
        runTest {
            probe.result = { ClientBookingProbeResult.Client("completed") }
            val client = resolver.resolve(PushDestination.LeaveReview("B1", PushAudience.Client), context())
            assertEquals(clientRoute(BookingSummaryKey("B1")), client)

            probe.result = { ClientBookingProbeResult.NotClient }
            val expert =
                resolver.resolve(PushDestination.LeaveReview("B1", PushAudience.Either), context(AppMode.Expert))
            assertEquals(expertRoute(expertSummary()), expert)
        }

    @Test
    fun leaveReviewNeverOpensUpcomingBooking() =
        runTest {
            probe.result = { ClientBookingProbeResult.Client("confirmed") }
            val resolved = resolver.resolve(PushDestination.LeaveReview("B1", PushAudience.Client), context())
            assertEquals(clientRoute(BookingSummaryKey("B1")), resolved)
        }

    @Test
    fun chatStaysInCurrentMode() =
        runTest {
            listOf(AppMode.Client, AppMode.Expert).forEach { mode ->
                val resolved =
                    resolver.resolve(PushDestination.ChatThread("T1"), context(mode, callActive = true))
                assertEquals(ResolvedPush(mode, PushRoute.Chat(ChatThreadKey("T1"))), resolved)
            }
        }

    private fun payoutsRoute() =
        ResolvedPush(
            AppMode.Expert,
            PushRoute.Shell(
                ShellRoute(AppMode.Expert, DashboardRoot, listOf(PayoutsKey(SettingsOrigin.Dashboard))),
            ),
        )

    @Test
    fun payoutsPushOpensPayoutsOnDashboard() =
        runTest {
            listOf(PushDestination.Payouts, PushDestination.KycOnboarding).forEach { destination ->
                assertEquals(payoutsRoute(), resolver.resolve(destination, context(AppMode.Expert)))
                assertEquals(payoutsRoute(), resolver.resolve(destination, context(AppMode.Client)))
            }
        }

    @Test
    fun payoutsPushForClientOnlyOpensList() =
        runTest {
            listOf(PushDestination.Payouts, PushDestination.KycOnboarding).forEach { destination ->
                val clientOnly = resolver.resolve(destination, context(AppMode.Client, hasExpertRole = false))
                assertEquals(clientList(), clientOnly)
                assertEquals(AppMode.Client, clientOnly.targetMode)
                assertEquals(clientList(), resolver.resolve(destination, context(AppMode.Client, callActive = true)))
                // Already in Expert mode, so no switch is needed even during a call.
                assertEquals(
                    payoutsRoute(),
                    resolver.resolve(destination, context(AppMode.Expert, callActive = true)),
                )
            }
        }

    @Test
    fun accountSecurityOpensProfileTab() =
        runTest {
            val client = resolver.resolve(PushDestination.AccountSecurity, context(AppMode.Client))
            assertEquals(
                ResolvedPush(
                    AppMode.Client,
                    PushRoute.Shell(ShellRoute(AppMode.Client, ClientProfileRoot, listOf(AccountSecurityKey))),
                ),
                client,
            )
            val expert = resolver.resolve(PushDestination.AccountSecurity, context(AppMode.Expert))
            assertEquals(
                ResolvedPush(
                    AppMode.Expert,
                    PushRoute.Shell(ShellRoute(AppMode.Expert, ExpertProfileRoot, listOf(AccountSecurityKey))),
                ),
                expert,
            )
        }

    @Test
    fun clientPushSwitchesExpertToClient() =
        runTest {
            probe.result = { ClientBookingProbeResult.Client("confirmed") }
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Client), context(AppMode.Expert))
            assertEquals(clientRoute(UpcomingBookingKey("B1")), resolved)
        }

    @Test
    fun expertPushSwitchesClientToExpertWithTheRole() =
        runTest {
            val resolved =
                resolver.resolve(PushDestination.BookingDetail("B1", PushAudience.Expert), context(AppMode.Client))
            assertEquals(expertRoute(expertSummary()), resolved)
        }

    @Test
    fun switchBlockedDuringCallOpensCurrentList() =
        runTest {
            probe.result = { ClientBookingProbeResult.Client("confirmed") }
            val resolved =
                resolver.resolve(
                    PushDestination.BookingDetail("B1", PushAudience.Client),
                    context(AppMode.Expert, callActive = true),
                )
            assertEquals(expertList(), resolved)
        }

    @Test
    fun noExpertRoleNeverSwitchesToExpert() =
        runTest {
            val resolved =
                resolver.resolve(
                    PushDestination.BookingDetail("B1", PushAudience.Expert),
                    context(AppMode.Client, hasExpertRole = false),
                )
            assertEquals(clientList(), resolved)
            assertEquals(AppMode.Client, resolved.targetMode)
        }

    @Test
    fun noExpertRoleNotClientProbeOpensClientList() =
        runTest {
            probe.result = { ClientBookingProbeResult.NotClient }
            val resolved =
                resolver.resolve(
                    PushDestination.BookingDetail("B1", PushAudience.Either),
                    context(AppMode.Client, hasExpertRole = false),
                )
            assertEquals(clientList(), resolved)
        }

    @Test
    fun requestInListOpensDecision() =
        runTest {
            val expiresAt = 1_800_000_000_000L
            val key = RequestDecisionKey("r1", isExpired = false, expiresAtMillis = expiresAt)
            requests.keys = mapOf("r1" to key)
            val resolved =
                resolver.resolve(PushDestination.IncomingRequests("r1"), context(AppMode.Expert))
            assertEquals(
                ResolvedPush(
                    AppMode.Expert,
                    PushRoute.Shell(ShellRoute(AppMode.Expert, DashboardRoot, listOf(IncomingRequestsKey, key))),
                ),
                resolved,
            )
        }

    @Test
    fun requestMissingOpensList() =
        runTest {
            val onlyList =
                ResolvedPush(
                    AppMode.Expert,
                    PushRoute.Shell(ShellRoute(AppMode.Expert, DashboardRoot, listOf(IncomingRequestsKey))),
                )
            assertEquals(onlyList, resolver.resolve(PushDestination.IncomingRequests("r1"), context(AppMode.Expert)))

            requests.keys = mapOf("r1" to RequestDecisionKey("r1"))
            requests.fail = true
            assertEquals(onlyList, resolver.resolve(PushDestination.IncomingRequests("r1"), context(AppMode.Expert)))

            requests.fail = false
            val noId = resolver.resolve(PushDestination.IncomingRequests(null), context(AppMode.Expert))
            assertEquals(onlyList, noId)
            assertEquals(listOf("r1", "r1"), requests.lookedUp)
        }

    @Test
    fun requestFromClientModeSwitchesWhenAllowed() =
        runTest {
            val key = RequestDecisionKey("r1", isExpired = false, expiresAtMillis = 42L)
            requests.keys = mapOf("r1" to key)
            val destination = PushDestination.IncomingRequests("r1")

            val switched = resolver.resolve(destination, context(AppMode.Client, hasExpertRole = true))
            assertEquals(
                ResolvedPush(
                    AppMode.Expert,
                    PushRoute.Shell(ShellRoute(AppMode.Expert, DashboardRoot, listOf(IncomingRequestsKey, key))),
                ),
                switched,
            )

            val noRole = resolver.resolve(destination, context(AppMode.Client, hasExpertRole = false))
            assertEquals(clientList(), noRole)
        }
}
