package com.uptrendinvestments.clickmeapp.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiRequestUrlsTest {
    @Test
    fun publicPaths_doNotNeedAuthToken() {
        listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset",
        ).forEach { path ->
            assertFalse(path, ApiRequestUrls.needAuthToken(path))
        }
    }

    @Test
    fun otherPaths_needAuthToken() {
        listOf(
            "/api/v1/auth/email/resend",
            "/api/v1/auth/email/verify",
            "/api/v1/auth/email/status",
            "/api/v1/expert/profile/setup",
            "/api/v1/meta/languages",
            "/api/v1/meta/expertise-tags",
            "/api/v1/user/profile/avatar",
            "/api/v1/me",
            "/api/v1/user/profile",
            "/api/v1/expert/topics",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
    }

    @Test
    fun queryString_doesNotChangeTheAnswer() {
        val path = "https://host/api/v1/auth/login?next=1".toHttpUrl().encodedPath
        assertFalse(ApiRequestUrls.needAuthToken(path))
    }

    @Test
    fun pathConstants_areRelative() {
        assertEquals("auth/login", ApiRequestUrls.LOGIN)
        assertEquals("auth/refresh", ApiRequestUrls.REFRESH)
        assertEquals("auth/password/forgot", ApiRequestUrls.FORGOT_PASSWORD)
        assertEquals("auth/password/reset", ApiRequestUrls.RESET_PASSWORD)
        assertEquals("me", ApiRequestUrls.ME)
        assertEquals("user/profile", ApiRequestUrls.USER_PROFILE)
        assertEquals("expert/profile", ApiRequestUrls.EXPERT_PROFILE)
        assertEquals("expert/topics", ApiRequestUrls.EXPERT_TOPICS)
    }

    @Test
    fun phaseFourConstants_haveExactRelativeValues() {
        assertEquals("auth/signup", ApiRequestUrls.SIGNUP)
        assertEquals("auth/email/resend", ApiRequestUrls.EMAIL_RESEND)
        assertEquals("auth/email/verify", ApiRequestUrls.EMAIL_VERIFY)
        assertEquals("auth/email/status", ApiRequestUrls.EMAIL_STATUS)
        assertEquals("expert/profile/setup", ApiRequestUrls.EXPERT_PROFILE_SETUP)
        assertEquals("meta/languages", ApiRequestUrls.META_LANGUAGES)
        assertEquals("meta/expertise-tags", ApiRequestUrls.META_EXPERTISE_TAGS)
        assertEquals("user/profile/avatar", ApiRequestUrls.USER_AVATAR)
    }

    @Test
    fun recoveryConstants_arePublic() {
        listOf(ApiRequestUrls.FORGOT_PASSWORD, ApiRequestUrls.RESET_PASSWORD).forEach { constant ->
            assertFalse(constant, ApiRequestUrls.needAuthToken("/api/v1/$constant"))
        }
    }

    @Test
    fun publicPathSet_hasExactlyFiveEntries() {
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun phaseFiveConstants_haveExactRelativeValues() {
        assertEquals("client/home", ApiRequestUrls.CLIENT_HOME)
        assertEquals("client/bookings", ApiRequestUrls.CLIENT_BOOKINGS)
        assertEquals("categories", ApiRequestUrls.CATEGORIES)
        assertEquals("experts/{id}/details", ApiRequestUrls.EXPERT_DETAILS)
        assertEquals("experts/{id}/reviews", ApiRequestUrls.EXPERT_REVIEWS)
    }

    @Test
    fun discoveryPaths_needAuthToken() {
        listOf(
            "/api/v1/client/home",
            "/api/v1/client/bookings",
            "/api/v1/categories",
            "/api/v1/experts/e1/details",
            "/api/v1/experts/e1/reviews",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
    }

    @Test
    fun phaseSixteenOneSearchConstant_isAuthed() {
        assertEquals("experts/search", ApiRequestUrls.EXPERTS_SEARCH)
        assertTrue(ApiRequestUrls.needAuthToken("/api/v1/experts/search"))
    }

    @Test
    fun phaseSixConstants_haveExactRelativeValues() {
        assertEquals("experts/{id}/availability", ApiRequestUrls.EXPERT_AVAILABILITY)
        assertEquals("bookings/request", ApiRequestUrls.BOOKING_REQUEST)
        assertEquals("client/bookings/{id}", ApiRequestUrls.CLIENT_BOOKING_DETAIL)
    }

    @Test
    fun bookingPaths_needAuthToken() {
        listOf(
            "/api/v1/experts/e1/availability",
            "/api/v1/bookings/request",
            "/api/v1/client/bookings/b1",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun phaseSevenConstants_haveExactRelativeValues() {
        assertEquals("me/payment-methods", ApiRequestUrls.PAYMENT_METHODS)
        assertEquals("me/payment-methods/setup-intent", ApiRequestUrls.PAYMENT_METHODS_SETUP_INTENT)
        assertEquals("me/payment-methods/{id}", ApiRequestUrls.PAYMENT_METHOD)
        assertEquals("me/payment-methods/{id}/default", ApiRequestUrls.PAYMENT_METHOD_DEFAULT)
        assertEquals("bookings/payment-intent", ApiRequestUrls.PAYMENT_INTENT)
        assertEquals("bookings/confirm", ApiRequestUrls.BOOKING_CONFIRM)
    }

    @Test
    fun paymentPaths_needAuthToken() {
        listOf(
            "/api/v1/me/payment-methods",
            "/api/v1/me/payment-methods/setup-intent",
            "/api/v1/me/payment-methods/pm_1",
            "/api/v1/me/payment-methods/pm_1/default",
            "/api/v1/bookings/payment-intent",
            "/api/v1/bookings/confirm",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun phaseEightConstants_haveExactRelativeValues() {
        assertEquals("client/bookings/{id}/cancel", ApiRequestUrls.CLIENT_BOOKING_CANCEL)
        assertEquals("client/bookings/{id}/accept-reschedule", ApiRequestUrls.CLIENT_BOOKING_ACCEPT_RESCHEDULE)
        assertEquals("client/bookings/{id}/decline-reschedule", ApiRequestUrls.CLIENT_BOOKING_DECLINE_RESCHEDULE)
    }

    @Test
    fun bookingActionPaths_needAuthToken() {
        listOf(
            "/api/v1/client/bookings/b1/cancel",
            "/api/v1/client/bookings/b1/accept-reschedule",
            "/api/v1/client/bookings/b1/decline-reschedule",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun phaseNineConstants_haveExactRelativeValues() {
        assertEquals("bookings/{id}/join", ApiRequestUrls.BOOKING_JOIN)
        assertEquals("bookings/{id}/confirm-connection", ApiRequestUrls.BOOKING_CONFIRM_CONNECTION)
        assertEquals("bookings/{id}/end", ApiRequestUrls.BOOKING_END)
    }

    @Test
    fun phaseTenConstants_haveExactRelativeValues() {
        assertEquals("expert/topics/{id}", ApiRequestUrls.EXPERT_TOPIC)
        assertEquals("expert/availability", ApiRequestUrls.EXPERT_MY_AVAILABILITY)
    }

    @Test
    fun expertTopicAndAvailabilityPaths_needAuthToken() {
        listOf(
            "/api/v1/expert/availability",
            "/api/v1/expert/topics/t1",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun callPaths_needAuthToken() {
        listOf(
            "/api/v1/bookings/b1/join",
            "/api/v1/bookings/b1/confirm-connection",
            "/api/v1/bookings/b1/end",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }

    @Test
    fun phaseElevenConstants_haveExactRelativeValues() {
        assertEquals("expert/booking-requests", ApiRequestUrls.EXPERT_BOOKING_REQUESTS)
        assertEquals("expert/booking-requests/{id}", ApiRequestUrls.EXPERT_BOOKING_REQUEST)
        assertEquals("expert/booking-requests/{id}/accept", ApiRequestUrls.EXPERT_BOOKING_REQUEST_ACCEPT)
        assertEquals("expert/booking-requests/{id}/decline", ApiRequestUrls.EXPERT_BOOKING_REQUEST_DECLINE)
        assertEquals("expert/bookings", ApiRequestUrls.EXPERT_BOOKINGS)
        assertEquals("expert/bookings/{id}/details", ApiRequestUrls.EXPERT_BOOKING_DETAILS)
        assertEquals("expert/bookings/{id}/cancel", ApiRequestUrls.EXPERT_BOOKING_CANCEL)
        assertEquals("expert/bookings/{id}/accept-reschedule", ApiRequestUrls.EXPERT_BOOKING_ACCEPT_RESCHEDULE)
        assertEquals("expert/bookings/{id}/decline-reschedule", ApiRequestUrls.EXPERT_BOOKING_DECLINE_RESCHEDULE)
        assertEquals("expert/earnings/weekly", ApiRequestUrls.EXPERT_EARNINGS_WEEKLY)
        assertEquals("expert/payouts/summary", ApiRequestUrls.EXPERT_PAYOUTS_SUMMARY)
        assertEquals("expert/connect/status", ApiRequestUrls.EXPERT_CONNECT_STATUS)
        assertEquals("expert/connect/onboard", ApiRequestUrls.EXPERT_CONNECT_ONBOARD)
        assertEquals("expert/connect/refresh-link", ApiRequestUrls.EXPERT_CONNECT_REFRESH_LINK)
    }

    @Test
    fun phaseSixteenTwoRescheduleConstants_areAuthed() {
        assertEquals("client/bookings/{id}/reschedule", ApiRequestUrls.CLIENT_BOOKING_RESCHEDULE)
        assertEquals("expert/bookings/{id}/reschedule", ApiRequestUrls.EXPERT_BOOKING_RESCHEDULE)
        assertTrue(ApiRequestUrls.needAuthToken("/api/v1/client/bookings/b1/reschedule"))
        assertTrue(ApiRequestUrls.needAuthToken("/api/v1/expert/bookings/b1/reschedule"))
    }

    @Test
    fun phaseSixteenThreeFavoritesConstants_areAuthed() {
        assertEquals("client/favorites", ApiRequestUrls.CLIENT_FAVORITES)
        assertEquals("client/favorites/{expertId}", ApiRequestUrls.CLIENT_FAVORITE)
        assertTrue(ApiRequestUrls.needAuthToken("/api/v1/client/favorites"))
        assertTrue(ApiRequestUrls.needAuthToken("/api/v1/client/favorites/0f1e2d3c-0000-4000-8000-000000000001"))
    }

    @Test
    fun phaseElevenPaths_needAuthToken() {
        listOf(
            "/api/v1/expert/booking-requests",
            "/api/v1/expert/booking-requests/r1",
            "/api/v1/expert/booking-requests/r1/accept",
            "/api/v1/expert/booking-requests/r1/decline",
            "/api/v1/expert/bookings",
            "/api/v1/expert/bookings/b1/details",
            "/api/v1/expert/bookings/b1/cancel",
            "/api/v1/expert/bookings/b1/accept-reschedule",
            "/api/v1/expert/bookings/b1/decline-reschedule",
            "/api/v1/expert/earnings/weekly",
            "/api/v1/expert/payouts/summary",
            "/api/v1/expert/connect/status",
            "/api/v1/expert/connect/onboard",
            "/api/v1/expert/connect/refresh-link",
        ).forEach { path ->
            assertTrue(path, ApiRequestUrls.needAuthToken(path))
        }
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }
}
