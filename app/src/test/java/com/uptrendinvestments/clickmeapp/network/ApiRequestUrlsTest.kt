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
    fun recoveryConstants_arePublic() {
        listOf(ApiRequestUrls.FORGOT_PASSWORD, ApiRequestUrls.RESET_PASSWORD).forEach { constant ->
            assertFalse(constant, ApiRequestUrls.needAuthToken("/api/v1/$constant"))
        }
    }

    @Test
    fun publicPathSet_hasExactlyFiveEntries() {
        assertEquals(5, ApiRequestUrls.publicPaths.size)
    }
}
