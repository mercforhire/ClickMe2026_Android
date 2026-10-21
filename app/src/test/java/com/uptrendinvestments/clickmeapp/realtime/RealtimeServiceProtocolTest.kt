package com.uptrendinvestments.clickmeapp.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.SocketFactory

/**
 * Engine.IO v4 / Socket.IO wire tests: the real socket.io-client 2.1.2 against a MockWebServer websocket.
 * Runs under Robolectric because the library needs the real `org.json`.
 */
@RunWith(RobolectricTestRunner::class)
class RealtimeServiceProtocolTest {
    /** The server side of one websocket connection. */
    private class ServerConn(
        private val rejectWith: String,
    ) : WebSocketListener() {
        val frames = CopyOnWriteArrayList<String>()

        @Volatile var ws: WebSocket? = null

        @Volatile var openedAtNanos = 0L

        override fun onOpen(
            webSocket: WebSocket,
            response: Response,
        ) {
            ws = webSocket
            openedAtNanos = System.nanoTime()
            webSocket.send(
                """0{"sid":"e1","upgrades":[],"pingInterval":25000,"pingTimeout":20000,"maxPayload":1000000}""",
            )
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String,
        ) {
            frames += text
            if (text.startsWith("40")) {
                if ("good" in text) {
                    webSocket.send("""40{"sid":"s1"}""")
                } else {
                    webSocket.send("""44{"message":"$rejectWith"}""")
                }
            }
        }

        fun send(frame: String) {
            checkNotNull(ws).send(frame)
        }
    }

    /** Records every TCP socket the client opens so a test can sever the transport without a close frame. */
    private class RecordingSocketFactory(
        private val sockets: MutableList<Socket>,
    ) : SocketFactory() {
        private val delegate = getDefault()

        override fun createSocket(): Socket = delegate.createSocket().also { sockets += it }

        override fun createSocket(
            host: String?,
            port: Int,
        ): Socket = delegate.createSocket(host, port).also { sockets += it }

        override fun createSocket(
            host: String?,
            port: Int,
            localHost: InetAddress?,
            localPort: Int,
        ): Socket = delegate.createSocket(host, port, localHost, localPort).also { sockets += it }

        override fun createSocket(
            host: InetAddress?,
            port: Int,
        ): Socket = delegate.createSocket(host, port).also { sockets += it }

        override fun createSocket(
            address: InetAddress?,
            port: Int,
            localAddress: InetAddress?,
            localPort: Int,
        ): Socket = delegate.createSocket(address, port, localAddress, localPort).also { sockets += it }
    }

    private lateinit var server: MockWebServer
    private val conns = CopyOnWriteArrayList<ServerConn>()
    private lateinit var service: RealtimeService
    private val clientSockets = CopyOnWriteArrayList<Socket>()

    @Volatile private var droppedAtNanos = 0L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = newService()
    }

    @After
    fun tearDown() {
        service.disconnect()
        conns.forEach { it.ws?.close(1000, "done") }
        server.close()
    }

    private fun newService(decoder: RealtimeEventDecoder = RealtimeEventDecoder()) =
        RealtimeService(
            client =
                OkHttpClient
                    .Builder()
                    .socketFactory(RecordingSocketFactory(clientSockets))
                    .build(),
            config =
                RealtimeConfig(
                    socketUrl = server.url("/").toString().trimEnd('/'),
                    reconnectDelayMs = 200,
                ),
            decoder = decoder,
        )

    private fun enqueueConn(rejectWith: String = "INVALID_TOKEN"): ServerConn {
        val conn = ServerConn(rejectWith)
        conns += conn
        server.enqueue(MockResponse.Builder().webSocketUpgrade(conn).build())
        return conn
    }

    private fun <T> blocking(block: suspend CoroutineScope.() -> T): T =
        runBlocking { withTimeout(TIMEOUT_MS) { block() } }

    /** Subscribes to [flow] first, runs [trigger], then returns the first value matching [predicate]. */
    private fun <T> awaitAfter(
        flow: Flow<T>,
        trigger: () -> Unit,
        predicate: (T) -> Boolean = { true },
    ): T =
        blocking {
            val pending = async(start = CoroutineStart.UNDISPATCHED) { flow.first(predicate) }
            trigger()
            pending.await()
        }

    /** Starts collecting [flow] into [into] and returns only once the subscription is registered. */
    private fun <T> collectInBackground(
        flow: Flow<T>,
        into: MutableList<T>,
    ): Job =
        CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
            flow.collect { into += it }
        }

    /** Kills the client's TCP socket without a close frame, like losing the network. */
    private fun dropTransport() {
        droppedAtNanos = System.nanoTime()
        clientSockets.last().close()
    }

    private fun awaitState(expected: ConnectionState) {
        blocking { service.state.first { it == expected } }
    }

    private fun connectAndAwait(token: String = "good-1") {
        service.connect(token)
        awaitState(ConnectionState.Connected)
    }

    private fun awaitFrame(
        conn: ServerConn,
        expected: String,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS)
        while (expected !in conn.frames) {
            check(System.nanoTime() < deadline) { "frame not received: $expected, got ${conn.frames}" }
            Thread.sleep(10)
        }
    }

    private fun awaitConnCount(count: Int) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS)
        while (server.requestCount < count) {
            check(System.nanoTime() < deadline) { "expected $count upgrades, got ${server.requestCount}" }
            Thread.sleep(10)
        }
    }

    private fun typingFrame(
        thread: String,
        sender: String,
    ) = """42["typing.start",{"thread_id":"$thread","sender_id":"$sender"}]"""

    @Test
    fun connectSendsTokenInConnectPacketAndBecomesConnected() {
        val conn = enqueueConn()
        service.connect("good-1")
        assertEquals(ConnectionState.Connecting, service.state.value)
        awaitState(ConnectionState.Connected)
        awaitFrame(conn, """40{"token":"good-1"}""")
        assertEquals("""40{"token":"good-1"}""", conn.frames.first())
    }

    @Test
    fun upgradeIsWebsocketOnlyWithoutTokenInUrlOrAuthorizationHeader() {
        enqueueConn()
        connectAndAwait()
        val request: RecordedRequest = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertTrue(request.url.encodedPath.startsWith("/socket.io/"))
        assertEquals("4", request.url.queryParameter("EIO"))
        assertEquals("websocket", request.url.queryParameter("transport"))
        assertNull(request.url.queryParameter("token"))
        assertFalse(request.url.toString().contains("good-1"))
        assertNull(request.headers["Authorization"])
        assertEquals("websocket", request.headers["Upgrade"]?.lowercase())
        assertEquals("only the websocket upgrade, no polling", 1, server.requestCount)
    }

    @Test
    fun typingStartFrameIsDeliveredAsTypedEvent() {
        val conn = enqueueConn()
        connectAndAwait()
        val event = awaitAfter(service.events, { conn.send(typingFrame("t1", "u2")) })
        assertEquals(RealtimeEvent.TypingStart("t1", "u2"), event)
    }

    @Test
    fun bookingUpdateKeepsStatusAsSent() {
        val conn = enqueueConn()
        connectAndAwait()
        val event =
            awaitAfter(service.events, {
                conn.send("""42["booking.update",{"booking_id":"b1","status":"rescheduled"}]""")
            })
        assertEquals(RealtimeEvent.BookingUpdate(bookingId = "b1", status = "rescheduled"), event)
    }

    @Test
    fun eventsHaveNoReplay() {
        val conn = enqueueConn()
        connectAndAwait()
        awaitAfter(service.events, { conn.send(typingFrame("t1", "u2")) })
        assertTrue(service.events.replayCache.isEmpty())
    }

    @Test
    fun invalidTokenRejectionBecomesHandshakeFailureAndIsNotRetried() {
        enqueueConn("INVALID_TOKEN")
        enqueueConn("INVALID_TOKEN")
        val failure = awaitAfter(service.handshakeFailures, { service.connect("bad-1") })
        assertEquals(HandshakeFailure(HandshakeFailureReason.InvalidToken, "bad-1"), failure)
        assertEquals(ConnectionState.Disconnected, service.state.value)
        Thread.sleep(1_500)
        assertEquals("no second upgrade after a middleware rejection", 1, server.requestCount)
    }

    @Test
    fun missingTokenRejectionIsClassified() {
        enqueueConn("MISSING_TOKEN")
        val failure = awaitAfter(service.handshakeFailures, { service.connect("bad-1") })
        assertEquals(HandshakeFailureReason.MissingToken, failure.reason)
        assertEquals(ConnectionState.Disconnected, service.state.value)
    }

    @Test
    fun accountSuspendedRejectionIsClassified() {
        enqueueConn("ACCOUNT_SUSPENDED")
        val failure = awaitAfter(service.handshakeFailures, { service.connect("bad-1") })
        assertEquals(HandshakeFailureReason.AccountSuspended, failure.reason)
        assertEquals(ConnectionState.Disconnected, service.state.value)
    }

    @Test
    fun connectingTwiceWithSameTokenWhileConnectedMakesOneUpgrade() {
        enqueueConn()
        enqueueConn()
        connectAndAwait()
        service.connect("good-1")
        Thread.sleep(500)
        assertEquals(1, server.requestCount)
        assertEquals(ConnectionState.Connected, service.state.value)
    }

    @Test
    fun disconnectStopsDeliveryFromTheOldSocket() {
        val conn = enqueueConn()
        connectAndAwait()
        val received = CopyOnWriteArrayList<RealtimeEvent>()
        val collector = collectInBackground(service.events, received)
        service.disconnect()
        assertEquals(ConnectionState.Disconnected, service.state.value)
        conn.ws?.send(typingFrame("t1", "u2"))
        Thread.sleep(500)
        assertTrue("no events after disconnect, got $received", received.isEmpty())
        assertEquals(ConnectionState.Disconnected, service.state.value)
        collector.cancel()
    }

    @Test
    fun reconnectsAfterTransportDropWithSameTokenAfterConfiguredDelay() {
        enqueueConn()
        val second = enqueueConn()
        connectAndAwait()
        val connecting =
            awaitAfter(service.state, { dropTransport() }, { it == ConnectionState.Connecting })
        assertEquals(ConnectionState.Connecting, connecting)
        awaitConnCount(2)
        awaitState(ConnectionState.Connected)
        awaitFrame(second, """40{"token":"good-1"}""")
        assertEquals("""40{"token":"good-1"}""", second.frames.first())
        val delayMs = TimeUnit.NANOSECONDS.toMillis(second.openedAtNanos - droppedAtNanos)
        assertTrue("reconnect delay was ${delayMs}ms", delayMs in 150..1500)
        val event = awaitAfter(service.events, { second.send(typingFrame("t9", "u9")) })
        assertEquals(RealtimeEvent.TypingStart("t9", "u9"), event)
    }

    @Test
    fun connectingWithNewTokenRebuildsSocketAndIgnoresTheOldOne() {
        val first = enqueueConn()
        val second = enqueueConn()
        connectAndAwait("good-1")
        service.connect("good-2")
        assertEquals(ConnectionState.Connecting, service.state.value)
        awaitState(ConnectionState.Connected)
        awaitFrame(second, """40{"token":"good-2"}""")
        assertEquals(2, server.requestCount)

        val received = CopyOnWriteArrayList<RealtimeEvent>()
        val collector = collectInBackground(service.events, received)
        first.ws?.send(typingFrame("old", "old"))
        second.send(typingFrame("new", "new"))
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS)
        while (received.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        Thread.sleep(300)
        collector.cancel()
        assertEquals(listOf<RealtimeEvent>(RealtimeEvent.TypingStart("new", "new")), received.toList())
        assertEquals(ConnectionState.Connected, service.state.value)
    }

    @Test
    fun oldSocketListenerAlreadyRunningWhenTokenRotatesDoesNotEmit() {
        service.disconnect()
        val rotated = AtomicBoolean(false)
        service =
            newService(
                object : RealtimeEventDecoder() {
                    override fun decode(
                        event: String,
                        json: String,
                    ): RealtimeEvent? {
                        // The old socket's listener is mid-flight when connect(newToken) swaps the socket.
                        if ("\"old\"" in json && rotated.compareAndSet(false, true)) {
                            val swap = Thread { service.connect("good-2") }
                            swap.start()
                            swap.join(TIMEOUT_MS)
                        }
                        return super.decode(event, json)
                    }
                },
            )
        val first = enqueueConn()
        val second = enqueueConn()
        connectAndAwait("good-1")

        val received = CopyOnWriteArrayList<RealtimeEvent>()
        val collector = collectInBackground(service.events, received)
        first.send(typingFrame("old", "old"))
        awaitFrame(second, """40{"token":"good-2"}""")
        awaitState(ConnectionState.Connected)
        second.send(typingFrame("new", "new"))
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS)
        while (received.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        Thread.sleep(300)
        collector.cancel()

        assertTrue("decoder hook never ran", rotated.get())
        assertEquals(listOf<RealtimeEvent>(RealtimeEvent.TypingStart("new", "new")), received.toList())
        assertEquals(ConnectionState.Connected, service.state.value)
    }

    @Test
    fun emitsGoOutOnlyWhileConnectedWithLowercasedIds() {
        val conn = enqueueConn()
        connectAndAwait()
        service.emitTypingStart("T1", "R1")
        val expectedTyping = """42["typing.start",{"thread_id":"t1","recipient_id":"r1"}]"""
        awaitFrame(conn, expectedTyping)
        service.emitMessageRead("t1", listOf("M1"))
        val expectedRead = """42["message.read",{"thread_id":"t1","message_ids":["m1"]}]"""
        awaitFrame(conn, expectedRead)
        service.emitTypingStop("T1", "R1")
        awaitFrame(conn, """42["typing.stop",{"thread_id":"t1","recipient_id":"r1"}]""")
    }

    @Test
    fun emptyMessageReadSendsNothing() {
        val conn = enqueueConn()
        connectAndAwait()
        service.emitMessageRead("t1", emptyList())
        service.emitTypingStop("T1", "R1")
        awaitFrame(conn, """42["typing.stop",{"thread_id":"t1","recipient_id":"r1"}]""")
        assertTrue(conn.frames.none { it.contains("message.read") })
    }

    @Test
    fun emitBeforeConnectIsNotBufferedForLater() {
        val conn = enqueueConn()
        service.emitTypingStart("T1", "R1")
        connectAndAwait()
        service.emitTypingStop("T1", "R1")
        awaitFrame(conn, """42["typing.stop",{"thread_id":"t1","recipient_id":"r1"}]""")
        assertTrue("stale typing.start was flushed: ${conn.frames}", conn.frames.none { it.contains("typing.start") })
    }

    @Test
    fun throwingDecoderNeverCrashesAndLaterEventsStillArrive() {
        service.disconnect()
        service =
            newService(
                object : RealtimeEventDecoder() {
                    override fun decode(
                        event: String,
                        json: String,
                    ): RealtimeEvent? {
                        if (event == RealtimeEventNames.BOOKING_UPDATE) throw RuntimeException("boom")
                        return super.decode(event, json)
                    }
                },
            )
        val conn = enqueueConn()
        val uncaught = CopyOnWriteArrayList<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, t -> uncaught += t }
        try {
            connectAndAwait()
            val event =
                awaitAfter(service.events, {
                    conn.send("""42["booking.update",{"booking_id":"b1","status":"confirmed"}]""")
                    conn.send(typingFrame("t1", "u2"))
                })
            assertEquals(RealtimeEvent.TypingStart("t1", "u2"), event)
            Thread.sleep(300)
            assertTrue("uncaught: $uncaught", uncaught.isEmpty())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }

    @Test
    fun malformedPayloadsAreDroppedAndLaterEventsStillArrive() {
        val conn = enqueueConn()
        connectAndAwait()
        val event =
            awaitAfter(service.events, {
                conn.send("""42["typing.start","oops"]""")
                conn.send("""42["message.new",{"message_id":"m1"}]""")
                conn.send("""42["user.status",{"user_id":"u1","status":"online"}]""")
            })
        assertEquals(RealtimeEvent.UserStatus("u1", "online"), event)
        assertEquals(ConnectionState.Connected, service.state.value)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
