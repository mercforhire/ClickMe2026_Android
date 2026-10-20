package com.uptrendinvestments.clickmeapp.realtime

import android.util.Log
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.time.Instant

/** [FakeRealtimeConnection] has no way to emit events, so this wraps it with an event bus of its own. */
private class EventingFakeConnection(
    private val base: FakeRealtimeConnection = FakeRealtimeConnection(),
) : RealtimeConnection by base {
    private val bus = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<RealtimeEvent> = bus

    fun setState(state: ConnectionState) = base.setState(state)

    fun emitFailure(failure: HandshakeFailure) = base.emitFailure(failure)

    fun emitEvent(event: RealtimeEvent) {
        check(bus.tryEmit(event)) { "event buffer full" }
    }
}

@RunWith(RobolectricTestRunner::class)
class RealtimeDebugLoggerTest {
    private val connection = EventingFakeConnection()

    private fun lines(): List<String> =
        ShadowLog
            .getLogsForTag("Realtime")
            .filter { it.type == Log.DEBUG }
            .map { it.msg }

    private fun awaitLine(predicate: (String) -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < deadline) {
            if (lines().any(predicate)) return
            Thread.sleep(10)
        }
        throw AssertionError("No matching Realtime log line within 5 s; saw ${lines()}")
    }

    @Test
    fun stateChangesAreLogged() {
        RealtimeDebugLogger.install(connection)
        connection.setState(ConnectionState.Connecting)
        awaitLine { it.contains("state=Connecting") }
        connection.setState(ConnectionState.Connected)
        awaitLine { it.contains("state=Connected") }
    }

    @Test
    fun messageNewLogsIdsButNotContent() {
        RealtimeDebugLogger.install(connection)
        connection.emitEvent(
            RealtimeEvent.MessageNew(
                messageId = "msg-123",
                threadId = "thr-9",
                senderId = "usr-1",
                type = "text",
                content = "top secret text",
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
                status = "sent",
            ),
        )
        awaitLine { it.contains("msg-123") }
        assertFalse(lines().any { it.contains("top secret text") })
    }

    @Test
    fun handshakeFailureLogsReasonButNotToken() {
        RealtimeDebugLogger.install(connection)
        connection.emitFailure(HandshakeFailure(HandshakeFailureReason.InvalidToken, "jwt-abc"))
        awaitLine { it.contains("InvalidToken") }
        assertFalse(lines().any { it.contains("jwt-abc") })
    }
}
