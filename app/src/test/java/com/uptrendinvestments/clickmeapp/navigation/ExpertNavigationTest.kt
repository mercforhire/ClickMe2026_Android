package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.ExpertCancelKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.showDashboard
import com.uptrendinvestments.clickmeapp.screens.expertmode.showExpertBookings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExpertNavigationTest {
    private val decision = RequestDecisionKey("r1")
    private val confirmed =
        BookingConfirmedKey(
            bookingId = "b1",
            clientName = "Sam",
            clientAvatarUrl = null,
            topic = "Tax",
            startMillis = 1_000L,
            endMillis = 4_600_000L,
            meetingType = "video",
            clientMessage = "Hello",
        )

    private fun newNavigator(): Navigator {
        val tabs = listOf<NavKey>(DashboardRoot, ExpertBookingsRoot, TopicsRoot, ExpertChatsRoot, ExpertProfileRoot)
        val state =
            NavigationState(
                startRoute = DashboardRoot,
                topLevelRoute = mutableStateOf<NavKey>(DashboardRoot),
                backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
            )
        return Navigator(state)
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun acceptReplacesDecisionWithConfirmed() {
        val navigator = newNavigator()
        navigator.navigate(IncomingRequestsKey)
        navigator.navigate(decision)
        navigator.replaceTop(confirmed)
        assertEquals(listOf<NavKey>(DashboardRoot, IncomingRequestsKey, confirmed), navigator.stack(DashboardRoot))
        navigator.goBack()
        assertEquals(listOf<NavKey>(DashboardRoot, IncomingRequestsKey), navigator.stack(DashboardRoot))
    }

    @Test
    fun backToHomeResetsDashboardFromAnyTab() {
        val navigator = newNavigator()
        navigator.navigate(IncomingRequestsKey)
        navigator.navigate(decision)
        navigator.navigate(ExpertBookingsRoot)
        assertEquals(ExpertBookingsRoot, navigator.state.topLevelRoute)
        navigator.showDashboard()
        assertEquals(listOf<NavKey>(DashboardRoot), navigator.stack(DashboardRoot))
        assertEquals(DashboardRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun finishedCancelFromDashboardOriginLandsOnBookings() {
        val navigator = newNavigator()
        navigator.navigate(ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Dashboard))
        navigator.navigate(ExpertCancelKey("b1", ExpertBookingOrigin.Dashboard))
        navigator.navigate(ExpertBookingsRoot)
        navigator.navigate(ExpertBookingSummaryKey("b2", ExpertBookingOrigin.Bookings))
        navigator.showExpertBookings()
        assertEquals(listOf<NavKey>(DashboardRoot), navigator.stack(DashboardRoot))
        assertEquals(listOf<NavKey>(ExpertBookingsRoot), navigator.stack(ExpertBookingsRoot))
        assertEquals(ExpertBookingsRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun originKeepsKeysDistinct() {
        assertNotEquals(
            ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Dashboard),
            ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Bookings),
        )
        assertNotEquals(
            ExpertCancelKey("b1", ExpertBookingOrigin.Dashboard),
            ExpertCancelKey("b1", ExpertBookingOrigin.Bookings),
        )
        assertNotEquals(AvailabilityKey(AvailabilityOrigin.Dashboard), AvailabilityKey(AvailabilityOrigin.Topics))
        assertNotEquals(AvailabilityKey(AvailabilityOrigin.Bookings), AvailabilityKey(AvailabilityOrigin.Profile))
    }

    @Test
    fun requestDecisionKeyDefaultsToPending() {
        assertEquals(false, RequestDecisionKey("r1").isExpired)
        assertEquals(null, RequestDecisionKey("r1").expiresAtMillis)
    }

    @Test
    fun keysRoundTripThroughSerialization() {
        val full = RequestDecisionKey("r1", true, 99L, 12.5, "CAD")
        assertEquals(full, roundTrip(RequestDecisionKey.serializer(), full))
        assertEquals(decision, roundTrip(RequestDecisionKey.serializer(), decision))
        assertEquals(confirmed, roundTrip(BookingConfirmedKey.serializer(), confirmed))
        val sparse = confirmed.copy(clientName = null, topic = null, meetingType = null, clientMessage = null)
        assertEquals(sparse, roundTrip(BookingConfirmedKey.serializer(), sparse))
        val summary = ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Bookings)
        assertEquals(summary, roundTrip(ExpertBookingSummaryKey.serializer(), summary))
        val cancel = ExpertCancelKey("b1", ExpertBookingOrigin.Dashboard)
        assertEquals(cancel, roundTrip(ExpertCancelKey.serializer(), cancel))
    }
}

/**
 * Round trip through the generated serializer using a minimal positional format, because the JSON format artifact is
 * not on the unit-test compile classpath (same approach as `TopicEditorKeyTest`).
 */
@OptIn(ExperimentalSerializationApi::class)
private fun <T> roundTrip(
    serializer: KSerializer<T>,
    value: T,
): T {
    val values = ArrayDeque<Any?>()
    serializer.serialize(ValueEncoder(values), value)
    return serializer.deserialize(ValueDecoder(values))
}

@OptIn(ExperimentalSerializationApi::class)
private class ValueEncoder(
    private val out: ArrayDeque<Any?>,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        out.addLast(value)
    }

    override fun encodeNull() {
        out.addLast(false)
    }

    override fun encodeNotNullMark() {
        out.addLast(true)
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int,
    ): CompositeEncoder {
        out.addLast(collectionSize)
        return this
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ValueDecoder(
    private val input: ArrayDeque<Any?>,
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = input.removeFirst()!!

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = decodeInt()

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}
