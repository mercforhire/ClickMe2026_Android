package com.uptrendinvestments.clickmeapp.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeConnectionContractsTest {
    @Test
    fun `classifies the three backend handshake codes`() {
        assertEquals(HandshakeFailureReason.MissingToken, classifyHandshakeMessage("MISSING_TOKEN"))
        assertEquals(HandshakeFailureReason.InvalidToken, classifyHandshakeMessage("INVALID_TOKEN"))
        assertEquals(HandshakeFailureReason.AccountSuspended, classifyHandshakeMessage("ACCOUNT_SUSPENDED"))
    }

    @Test
    fun `null and unknown handshake messages classify as other`() {
        assertEquals(HandshakeFailureReason.Other, classifyHandshakeMessage(null))
        assertEquals(HandshakeFailureReason.Other, classifyHandshakeMessage("WHATEVER"))
    }

    @Test
    fun `handshake failure toString never prints the token`() {
        val text = HandshakeFailure(HandshakeFailureReason.InvalidToken, "secret-jwt").toString()
        assertFalse(text.contains("secret-jwt"))
        assertTrue(text.contains("InvalidToken"))
    }

    @Test
    fun `realtime config defaults match the contract`() {
        val config = RealtimeConfig("u")
        assertEquals(2_000L, config.reconnectDelayMs)
        assertEquals(2_000L, config.retryInitialDelayMs)
        assertEquals(30_000L, config.retryMaxDelayMs)
    }

    @Test
    fun `connection state has the three documented values`() {
        assertEquals(
            listOf(ConnectionState.Disconnected, ConnectionState.Connecting, ConnectionState.Connected),
            ConnectionState.entries,
        )
    }
}
