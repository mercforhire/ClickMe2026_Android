package com.uptrendinvestments.clickmeapp.realtime

import com.uptrendinvestments.clickmeapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeUrlTest {
    @Test
    fun `strips api v1 from the production base url`() {
        assertEquals(
            "https://clickme-backend-production.up.railway.app",
            socketUrl("https://clickme-backend-production.up.railway.app/api/v1"),
        )
    }

    @Test
    fun `strips api v1 when the base url has a trailing slash`() {
        assertEquals(
            "https://clickme-backend-production.up.railway.app",
            socketUrl("https://clickme-backend-production.up.railway.app/api/v1/"),
        )
    }

    @Test
    fun `keeps a non default port and the http scheme`() {
        assertEquals("http://10.0.2.2:3000", socketUrl("http://10.0.2.2:3000/api/v1/"))
    }

    @Test
    fun `keeps the loopback hosts of the local test backend`() {
        assertEquals("http://localhost:3000", socketUrl("http://localhost:3000/api/v1"))
        assertEquals("http://127.0.0.1:3000", socketUrl("http://127.0.0.1:3000/api/v1/"))
    }

    @Test
    fun `drops the port when it is the scheme default`() {
        assertEquals("https://host", socketUrl("https://host:443/api/v1"))
    }

    @Test
    fun `the build config base url is https unless it is the loopback test backend and has no api path`() {
        val url = socketUrl(BuildConfig.BASE_URL)
        val host = java.net.URI(url).host
        assertTrue(url.startsWith("https://") || host == "localhost" || host == "127.0.0.1")
        assertFalse(url.contains("/api"))
    }
}
