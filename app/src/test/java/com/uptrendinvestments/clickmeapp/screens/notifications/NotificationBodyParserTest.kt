package com.uptrendinvestments.clickmeapp.screens.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationBodyParserTest {
    @Test
    fun aMatchedPairBecomesABoldSpan() {
        assertEquals(
            listOf(
                BodySpan("Your session with ", false),
                BodySpan("Ada", true),
                BodySpan(" is confirmed.", false),
            ),
            parseNotificationBody("Your session with **Ada** is confirmed."),
        )
    }

    @Test
    fun severalPairsAreEachBold() {
        assertEquals(
            listOf(BodySpan("a", true), BodySpan(" and ", false), BodySpan("b", true)),
            parseNotificationBody("**a** and **b**"),
        )
    }

    @Test
    fun unmatchedMarkersStayLiteral() {
        assertEquals(listOf(BodySpan("5 ** 3", false)), parseNotificationBody("5 ** 3"))
        assertEquals(listOf(BodySpan("**open", false)), parseNotificationBody("**open"))
    }

    @Test
    fun anEmptyPairStaysLiteral() {
        assertEquals(listOf(BodySpan("****", false)), parseNotificationBody("****"))
        assertEquals(listOf(BodySpan("a **** b", false)), parseNotificationBody("a **** b"))
    }

    @Test
    fun emptyBodyHasNoSpans() {
        assertEquals(emptyList<BodySpan>(), parseNotificationBody(""))
    }

    @Test
    fun stripRemovesOnlyMatchedMarkers() {
        assertEquals("Ada said hi", stripBoldMarkers("**Ada** said **hi**"))
        assertEquals("5 ** 3", stripBoldMarkers("5 ** 3"))
        assertEquals("****", stripBoldMarkers("****"))
    }

    @Test
    fun realBackendBodiesAreUnchanged() {
        listOf(
            "You received a new review.",
            "Your booking was confirmed",
            "A client has requested a booking with you.",
            "Your booking has been confirmed by the expert.",
            "Your session has ended.",
            "Your booking request was not accepted in time and has expired.",
        ).forEach { body ->
            assertEquals(listOf(BodySpan(body, false)), parseNotificationBody(body))
            assertEquals(body, stripBoldMarkers(body))
        }
    }
}
