package com.uptrendinvestments.clickmeapp.realtime

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * socket.io-client 2.1.2 implementation of [RealtimeConnection].
 *
 * One fresh `forceNew` socket is built per connection (a back-to-back `disconnect()`/`connect()` on the
 * same socket silently fails), and a generation counter makes callbacks from a superseded socket no-ops.
 * The JWT travels only in the Socket.IO CONNECT packet (`IO.Options.auth`), never in the URL, and the
 * upgrade uses the bare client so no bearer header is attached. Every listener is wrapped in a catch-all
 * because the library rethrows listener exceptions on its EventThread, which would kill the process.
 * Logs contain only event names and exception class names.
 */
@Singleton
class RealtimeService
    @Inject
    constructor(
        @Named("bare") private val client: OkHttpClient,
        private val config: RealtimeConfig,
        private val decoder: RealtimeEventDecoder,
    ) : RealtimeConnection {
        private val lock = Any()
        private var socket: Socket? = null
        private var activeToken: String? = null

        @Volatile
        private var generation = 0

        private val _events =
            MutableSharedFlow<RealtimeEvent>(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        override val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

        private val _state = MutableStateFlow(ConnectionState.Disconnected)
        override val state: StateFlow<ConnectionState> = _state.asStateFlow()

        private val failureBus =
            MutableSharedFlow<HandshakeFailure>(
                replay = 0,
                extraBufferCapacity = 8,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        override val handshakeFailures: SharedFlow<HandshakeFailure> = failureBus.asSharedFlow()

        override fun connect(token: String) {
            synchronized(lock) {
                if (token == activeToken && _state.value != ConnectionState.Disconnected) return
                teardownLocked()
                val gen = ++generation
                val options =
                    IO.Options().apply {
                        forceNew = true
                        transports = arrayOf("websocket")
                        reconnection = true
                        reconnectionAttempts = Int.MAX_VALUE
                        reconnectionDelay = config.reconnectDelayMs
                        // Equal min and max remove backoff growth and jitter: exactly the configured delay.
                        reconnectionDelayMax = config.reconnectDelayMs
                        callFactory = client
                        webSocketFactory = client
                        auth = mapOf("token" to token)
                    }
                val newSocket = IO.socket(URI.create(config.socketUrl), options)
                bind(newSocket, gen, token)
                socket = newSocket
                activeToken = token
                _state.value = ConnectionState.Connecting
                newSocket.connect()
            }
        }

        override fun disconnect() {
            synchronized(lock) {
                teardownLocked()
                _state.value = ConnectionState.Disconnected
            }
        }

        override fun emitTypingStart(
            threadId: String,
            recipientId: String,
        ) = emitTyping(RealtimeEventNames.TYPING_START, threadId, recipientId)

        override fun emitTypingStop(
            threadId: String,
            recipientId: String,
        ) = emitTyping(RealtimeEventNames.TYPING_STOP, threadId, recipientId)

        override fun emitMessageRead(
            threadId: String,
            messageIds: List<String>,
        ) {
            val payload = RealtimeEmits.messageReadPayload(threadId, messageIds) ?: return
            send(RealtimeEventNames.MESSAGE_READ, payload)
        }

        private fun emitTyping(
            event: String,
            threadId: String,
            recipientId: String,
        ) = send(event, RealtimeEmits.typingPayload(threadId, recipientId))

        /** Only while Connected: the library would otherwise buffer the emit and flush it stale later. */
        private fun send(
            event: String,
            payload: String,
        ) {
            try {
                val current =
                    synchronized(lock) {
                        if (_state.value != ConnectionState.Connected) return
                        socket
                    }
                current?.emit(event, JSONObject(payload))
            } catch (t: Throwable) {
                Log.w(TAG, "emit failed: ${t.javaClass.simpleName}")
            }
        }

        private fun teardownLocked() {
            // Invalidates callbacks of the old socket.
            generation++
            socket?.let {
                it.off()
                it.disconnect()
            }
            socket = null
            activeToken = null
        }

        private fun bind(
            s: Socket,
            gen: Int,
            token: String,
        ) {
            // The generation check and the side effect run under the same lock as connect()/disconnect(), so a
            // listener of a superseded socket that is already running cannot write state or emit after the
            // swap. Nothing holding the lock waits on the EventThread (connect/disconnect only post to it).
            fun guarded(block: (Array<Any?>) -> Unit) =
                Emitter.Listener { args ->
                    try {
                        synchronized(lock) { if (gen == generation) block(args) }
                    } catch (t: Throwable) {
                        Log.w(TAG, "listener failed: ${t.javaClass.simpleName}")
                    }
                }

            s.on(Socket.EVENT_CONNECT, guarded { _state.value = ConnectionState.Connected })
            s.on(
                Socket.EVENT_DISCONNECT,
                guarded { args ->
                    if (args.firstOrNull() == IO_SERVER_DISCONNECT) {
                        // The library destroys the socket and will not reconnect: hand over to the manager.
                        _state.value = ConnectionState.Disconnected
                        failureBus.tryEmit(HandshakeFailure(HandshakeFailureReason.ServerDisconnected, token))
                    } else {
                        // The library's own reconnect loop is running.
                        _state.value = ConnectionState.Connecting
                    }
                },
            )
            s.on(
                Socket.EVENT_CONNECT_ERROR,
                guarded { args ->
                    // JSONObject = server middleware rejection (terminal); Exception = transport error (library retries).
                    (args.firstOrNull() as? JSONObject)?.let {
                        _state.value = ConnectionState.Disconnected
                        failureBus.tryEmit(HandshakeFailure(classifyHandshakeMessage(it.optString("message")), token))
                    }
                },
            )
            for (name in RealtimeEventNames.INBOUND) {
                s.on(name) { args ->
                    try {
                        // Decode outside the lock; only the generation check and the emit are atomic.
                        val json = args.firstOrNull() as? JSONObject
                        val event = json?.let { decoder.decode(name, it.toString()) }
                        if (event != null) {
                            synchronized(lock) { if (gen == generation) _events.tryEmit(event) }
                        } else {
                            Log.d(TAG, "dropped $name")
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "listener failed: ${t.javaClass.simpleName}")
                    }
                }
            }
        }

        private companion object {
            const val TAG = "Realtime"
            const val IO_SERVER_DISCONNECT = "io server disconnect"
        }
    }
