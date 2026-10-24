package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.model.enums.EndReason
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmConnectionRequest
import com.uptrendinvestments.clickmeapp.model.requests.EndCallRequest
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.realtime.ProcessForeground
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped owner of the live call. Port of iOS `CallCenter` + `MeetingCallViewModel`: the UI mounts and
 * unmounts freely while this singleton keeps the session (D-08).
 *
 * Main-confined: every state transition runs on `Dispatchers.Main.immediate`, and public functions are
 * called from the UI on Main. Never logs: the RTC token passes through the local start scope straight into
 * [CallEngine.join] and is never stored (T-09-02, T-09-16). Does not inject the user manager (Hilt cycle
 * through the session cleaner set, RESEARCH Pitfall 8).
 *
 * Start order for the in-app path (RESEARCH Pattern 3): agora_config check, microphone permission,
 * visible-app check, `startForeground` with the microphone type, engine join. A failure at any step tears
 * the container down without `/end` because no channel was joined (D-04, iOS `abortCallOnJoinFailure`).
 *
 * Status is derived from the two sides' connection states. Agora `onUserOffline` is the primary "peer left"
 * signal; the realtime `call.ended` echo is a second source for the same booking. The realtime socket is
 * down while the app is backgrounded unless the keep-alive from plan 09-11 holds it (RESEARCH Pitfall 6).
 * The echo race in Pitfall 7 is accepted for iOS parity (no time-window heuristic).
 *
 * Hang-up, the cutoff and logout teardown live in this class too. Hang-up leaves the channel, then sends
 * `POST /end` for every leg that started (never branching on its outcome), then destroys the engine and stops
 * the service; the user can rejoin for the whole window (CALL-08, CALL-END-SEMANTICS). A hang-up before any
 * leg started sends no `/end` (WR-02). The iOS `isRejoin` parameter is
 * obsolete after CALL-END-SEMANTICS and is not ported (RESEARCH Deprecated).
 *
 * It is also a [SessionDataCleaner]: a forced logout or suspension tears the call down locally, with no
 * `/end` (the tokens are already gone) and no dialog (RESEARCH Pitfall 8).
 */
@Singleton
class CallCenter
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val realtime: RealtimeConnection,
        private val engine: CallEngine,
        private val foreground: CallForegroundController,
        private val permissionGate: CallPermissionGate,
        private val processForeground: ProcessForeground,
        private val clock: Clock,
        @ApplicationScope private val scope: CoroutineScope,
    ) : CallGuard,
        CallStarter,
        SessionDataCleaner {
        private val _session = MutableStateFlow<CallSession?>(null)

        /** The live call, or null. */
        val session: StateFlow<CallSession?> = _session.asStateFlow()

        private val _isMinimized = MutableStateFlow(false)

        /** True when the call UI is collapsed to the in-app banner. */
        val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

        private val _dialogs = MutableStateFlow<List<CallDialog>>(emptyList())

        /** Queued dialogs; the root overlay renders the first. */
        val dialogs: StateFlow<List<CallDialog>> = _dialogs.asStateFlow()

        private val _isCallActive = MutableStateFlow(false)
        override val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

        private var nextSessionId = 0L
        private var startJob: Job? = null
        private var cutoffJob: Job? = null
        private var endJob: Job? = null

        /** The current session's confirm-connection PATCH; `/end` waits for it so it never overtakes it (WR-03). */
        private var confirmJob: Job? = null

        /** True once the engine was asked to join, so teardown leaves and destroys only a touched engine. */
        private var engineEngaged = false

        init {
            scope.launch(Dispatchers.Main.immediate) { engine.events.collect(::onEngineEvent) }
            // D-10: the notification's End call action runs the same flow as the button.
            scope.launch(Dispatchers.Main.immediate) { foreground.hangUpRequests.collect { endCall() } }
            scope.launch(Dispatchers.Main.immediate) {
                realtime.events.filterIsInstance<RealtimeEvent.CallEnded>().collect(::onPeerCallEnded)
            }
        }

        // region Start

        override fun startCall(request: CallStartRequest) {
            if (_session.value != null) {
                enqueue(CallDialog.Guard(GuardMessage.AlreadyOnCall))
                return
            }
            val id = ++nextSessionId
            engineEngaged = false
            // A confirm still in flight for the previous session must not block this session's confirm.
            confirmJob?.cancel()
            confirmJob = null
            val session =
                CallSession(
                    id = id,
                    bookingId = request.bookingId,
                    peerId = request.peerId,
                    peerName = request.peerName,
                    peerAvatarUrl = request.peerAvatarUrl,
                    topic = request.topic,
                    scheduledStart = request.scheduledStart,
                    scheduledEnd = request.scheduledEnd,
                    connectionType = request.initialConnectionType(),
                )
            setSession(session)
            _isMinimized.value = false
            // startJob must exist before the cutoff can fire: a cutoff that is already due ends the call at
            // once, and the end flow has to find and cancel this start (WR-01).
            startJob = scope.launch(Dispatchers.Main.immediate) { runStart(id, request) }
            startCutoff(session)
        }

        /**
         * True while [id] is the current session and no end flow has begun. Every start step re-checks it after
         * a suspension, so a start never outlives a hang-up, the cutoff or a teardown (WR-01).
         */
        private fun isLive(id: Long): Boolean = _session.value?.let { it.id == id && !it.isEndingCall } == true

        private suspend fun runStart(
            id: Long,
            request: CallStartRequest,
        ) {
            val response =
                try {
                    apiCall { api.joinCall(request.bookingId) }.data
                } catch (e: ApiException) {
                    abortStart(id, CallDialog.JoinError(e.toJoinErrorReason()))
                    return
                }
            if (!isLive(id)) return
            // The server response is the source of truth over the client hint (D-05).
            val type = ConnectionType.fromWire(response.connectionType)
            if (type == null) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown))
                return
            }
            updateSession(id) { it.copy(connectionType = type) }
            when (type) {
                ConnectionType.SkypeZoom -> startExternal(id, response)
                ConnectionType.InAppVoice -> startInApp(id, request, response)
            }
        }

        private suspend fun startExternal(
            id: Long,
            response: JoinCallResponse,
        ) {
            val url = response.externalConfig?.joinUrl?.takeIf { it.isNotBlank() }
            if (url == null) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.MissingMeetingLink))
                return
            }
            val now = clock.instant()
            updateSession(id) { it.copy(externalJoinUrl = url, joinStartedAt = now) }
        }

        private suspend fun startInApp(
            id: Long,
            request: CallStartRequest,
            response: JoinCallResponse,
        ) {
            val config = response.agoraConfig
            if (config == null) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.NotSetUpForInAppVoice))
                return
            }
            val permission = permissionGate.ensureMicrophone()
            if (!isLive(id)) return
            when (permission) {
                MicPermissionResult.Granted -> Unit
                MicPermissionResult.DeniedPermanently -> {
                    abortStart(id, CallDialog.MicPermissionDenied)
                    return
                }
                MicPermissionResult.DeniedOnce, MicPermissionResult.Cancelled -> {
                    abortStart(id, null)
                    return
                }
            }
            // A microphone foreground service must never start from the background (RESEARCH Pitfall 1).
            if (!processForeground.isForeground.value) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown))
                return
            }
            val started =
                foreground.startAndAwait(
                    CallNotificationInfo(request.bookingId, request.peerName, request.scheduledStart),
                )
            if (!isLive(id)) return
            if (!started) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown))
                return
            }
            engineEngaged = true
            val joined = engine.join(config.rtcToken, config.channelName, config.uid)
            if (!isLive(id)) {
                // An end flow running for this session owns the teardown (it waits for this start, then leaves
                // and destroys the engaged engine). A session that was cleared without that flow leaves nobody
                // else to release the engine, so release it here instead of leaking a publishing channel.
                if (_session.value == null) releaseOrphanEngine()
                return
            }
            if (!joined) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown), engineTouched = true)
                return
            }
            val now = clock.instant()
            updateSession(id) { it.copy(joinStartedAt = now) }
        }

        /** Leaves and destroys an engine whose session is already gone (defensive, WR-01). */
        private suspend fun releaseOrphanEngine() {
            withContext(NonCancellable) {
                withTimeoutOrNull(LEAVE_STEP_TIMEOUT_MS) { engine.leave() }
                engine.destroy()
            }
            engineEngaged = false
        }

        /**
         * Tears the container down after a failed start. No `/end`: the user never joined a channel (D-04).
         * Does nothing when the session was already replaced or cleared, or when an end flow already owns the
         * teardown.
         */
        private suspend fun abortStart(
            id: Long,
            dialog: CallDialog?,
            engineTouched: Boolean = false,
        ) {
            if (!isLive(id)) return
            cutoffJob?.cancel()
            if (engineTouched) engine.destroy()
            engineEngaged = false
            foreground.stop()
            setSession(null)
            _isMinimized.value = false
            dialog?.let(::enqueue)
        }

        // endregion

        // region Engine and realtime events

        private fun onEngineEvent(event: EngineEvent) {
            val current = _session.value ?: return
            if (current.connectionType != ConnectionType.InAppVoice || current.isEndingCall) return
            var mine = current.mine
            var remote = current.remote
            when (event) {
                EngineEvent.LocalJoined -> mine = ConnState.Ready
                EngineEvent.LocalFailed -> mine = ConnState.Disconnected
                is EngineEvent.RemoteJoined -> remote = ConnState.Ready
                is EngineEvent.RemoteOffline -> remote = ConnState.Disconnected
                is EngineEvent.Error -> return
            }
            applyConnStates(current, mine, remote)
        }

        /**
         * `call.ended` for the live booking means the peer left, never that this call ends (CALL-06,
         * CALL-END-SEMANTICS): it is modelled as remote Disconnected so a rejoin restores Active.
         */
        private fun onPeerCallEnded(event: RealtimeEvent.CallEnded) {
            val current = _session.value ?: return
            if (event.bookingId != current.bookingId || current.isEndingCall) return
            applyConnStates(current, current.mine, ConnState.Disconnected)
        }

        private fun applyConnStates(
            current: CallSession,
            mine: ConnState,
            remote: ConnState,
        ) {
            val next = current.withConnStates(mine, remote, clock.instant())
            setSession(next)
            if (next.status == CallStatus.Active) confirmConnectionOnce()
        }

        /**
         * Sends `PATCH confirm-connection` until it succeeds once per session, with at most one request in
         * flight. [CallSession.confirmSent] is set only when the PATCH succeeded, so `/end` reports `completed`
         * only for a booking the server moved to `in_progress`, and a failed PATCH is retried on the next Active
         * transition or link open (WR-03; iOS sets its flag before the request, RESEARCH Pitfall 9). A failure
         * never interrupts the call (CALL-05).
         */
        private fun confirmConnectionOnce() {
            val current = _session.value ?: return
            if (current.confirmSent || confirmJob?.isActive == true) return
            val id = current.id
            val bookingId = current.bookingId
            val body =
                ConfirmConnectionRequest(
                    timestamp = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(),
                    method = current.connectionType.wire,
                )
            confirmJob =
                scope.launch(Dispatchers.Main.immediate) {
                    val confirmed =
                        try {
                            apiCall { api.confirmCallConnection(bookingId, body) }
                            true
                        } catch (_: ApiException) {
                            // 409 and every other error leave the flag unset; the call carries on.
                            false
                        }
                    if (confirmed) updateSession(id) { it.copy(confirmSent = true) }
                }
        }

        // endregion

        // region Controls

        /** Mutes or unmutes the microphone. The flag changes only when the engine accepts the request. */
        fun toggleMute() {
            val current = _session.value ?: return
            if (current.connectionType != ConnectionType.InAppVoice) return
            val target = !current.isMuted
            if (engine.setMuted(target)) setSession(current.copy(isMuted = target))
        }

        /** Switches between loudspeaker and earpiece. The flag changes only when the engine accepts it. */
        fun toggleSpeaker() {
            val current = _session.value ?: return
            if (current.connectionType != ConnectionType.InAppVoice) return
            val target = !current.isSpeakerOn
            if (engine.setSpeakerOn(target)) setSession(current.copy(isSpeakerOn = target))
        }

        /** The user opened the external meeting link: confirm once (method skype_zoom) and minimize (D-05). */
        fun onExternalLinkOpened() {
            val current = _session.value ?: return
            if (current.isEndingCall) return
            if (current.connectionType != ConnectionType.SkypeZoom || current.externalJoinUrl == null) return
            setSession(current.copy(hasOpenedExternalLink = true))
            confirmConnectionOnce()
            minimize()
        }

        fun minimize() {
            if (_session.value == null) return
            _isMinimized.value = true
        }

        fun expand() {
            if (_session.value == null) return
            _isMinimized.value = false
        }

        // endregion

        // region Hang-up, cutoff and logout teardown

        /** Hang-up from the button; the notification action arrives through the foreground controller. */
        fun endCall() {
            endCall(forced = false)
        }

        /**
         * Always runs in a fresh coroutine, so the cutoff loop or the hang-up collector that calls it is
         * never cancelled by the end flow itself.
         */
        private fun endCall(forced: Boolean) {
            val current = _session.value ?: return
            if (current.isEndingCall) return
            endJob = scope.launch(Dispatchers.Main.immediate) { performEnd(forced) }
        }

        /**
         * Leave the channel, `POST /end` (only when a leg started), then destroy, stop the service and clear
         * (CALL-08, D-10). The foreground service stays up until `/end` finished or timed out so the process
         * survives the request.
         */
        private suspend fun performEnd(forced: Boolean) {
            val started = _session.value ?: return
            if (started.isEndingCall) return
            val id = started.id
            setSession(
                started.copy(
                    isEndingCall = true,
                    status = CallStatus.Ended,
                    endedAt = started.endedAt ?: clock.instant(),
                ),
            )
            cutoffJob?.cancel()
            try {
                // An in-flight engine join finishes before leave.
                startJob?.cancelAndJoin()
                val joined = _session.value?.takeIf { it.id == id } ?: return
                if (joined.connectionType == ConnectionType.InAppVoice && engineEngaged) {
                    // The peer hears us go immediately.
                    withTimeoutOrNull(LEAVE_STEP_TIMEOUT_MS) { engine.leave() }
                }
                // `completed` is accepted only once the confirm PATCH moved the booking to in_progress, so an
                // in-flight confirm settles (bounded) before the reason is chosen and `/end` is sent (WR-03).
                withTimeoutOrNull(CONFIRM_SETTLE_TIMEOUT_MS) { confirmJob?.join() }
                val current = _session.value?.takeIf { it.id == id } ?: return
                // Read after the join and the confirm settled, so only a confirm that succeeded counts.
                val reason = if (current.confirmSent) EndReason.Completed else EndReason.ManualExit
                // No leg started (hang-up while /join, the mic prompt or the service start was in flight): no
                // `/end`. iOS sends max(0, d) = 0 here, which the backend's `.int().positive()` schema rejects
                // with a 400, so nothing is recorded there either. Sending a clamped 1 s would overwrite the
                // booking's actual_duration_secs / end_reason from a real earlier leg and emit call.ended to
                // the peer (WR-02).
                val joinedAt = current.joinStartedAt
                if (joinedAt != null) {
                    // A real sub-second leg still reports 1 s: the schema rejects 0.
                    val actualDuration =
                        Duration
                            .between(joinedAt, clock.instant())
                            .seconds
                            .coerceIn(1, Int.MAX_VALUE.toLong())
                            .toInt()
                    // Sent for both variants, never branching on the response or its status: a 409 on
                    // `completed` is expected (CALL-END-SEMANTICS, RESEARCH Pitfall 9).
                    withTimeoutOrNull(END_REQUEST_TIMEOUT_MS) {
                        try {
                            apiCall { api.endCall(current.bookingId, EndCallRequest(actualDuration, reason.wire)) }
                        } catch (_: ApiException) {
                            // Ignored: teardown continues whatever the server said.
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) { finishEnd(id, forced) }
            }
        }

        private suspend fun finishEnd(
            id: Long,
            forced: Boolean,
        ) {
            val current = _session.value?.takeIf { it.id == id } ?: return
            if (current.connectionType == ConnectionType.InAppVoice && engineEngaged) engine.destroy()
            engineEngaged = false
            foreground.stop()
            setSession(null)
            _isMinimized.value = false
            if (forced) enqueue(CallDialog.TimeLimit)
        }

        /**
         * Force-ends the call at [overtimeCutoff] once (CALL-07). Lives here, never in a UI ticker (the ticker
         * stops when nothing is composed). `delay` uses uptime, which stalls in deep sleep, so the loop re-reads
         * the injected wall clock at most every [CUTOFF_POLL_MS] (RESEARCH Pitfall 11). The 3600 s limit
         * replaces the iOS five-minute TEMP value.
         */
        private fun startCutoff(session: CallSession) {
            val end = session.scheduledEnd ?: return
            val cutoff = overtimeCutoff(end)
            cutoffJob =
                scope.launch(Dispatchers.Main.immediate) {
                    while (isActive) {
                        val left = Duration.between(clock.instant(), cutoff).toMillis()
                        if (left <= 0) {
                            endCall(forced = true)
                            break
                        }
                        delay(minOf(left, CUTOFF_POLL_MS))
                    }
                }
        }

        /**
         * Forced logout or suspension: tear the call down locally. No `/end` (UserManager clears the tokens
         * before cleaners run, so it would 401) and no dialog; the booking stays live and the next session
         * can rejoin (RESEARCH Pitfall 8). User-initiated logout is guarded and never gets here mid-call.
         */
        override suspend fun clearSessionData() {
            withContext(Dispatchers.Main.immediate + NonCancellable) { localTeardown() }
        }

        private suspend fun localTeardown() {
            cutoffJob?.cancel()
            startJob?.cancelAndJoin()
            // A running end flow finishes its own teardown in its finally block.
            endJob?.cancelAndJoin()
            val current = _session.value
            if (current != null) {
                if (current.connectionType == ConnectionType.InAppVoice && engineEngaged) {
                    withTimeoutOrNull(LEAVE_STEP_TIMEOUT_MS) { engine.leave() }
                    engine.destroy()
                }
                engineEngaged = false
                foreground.stop()
                setSession(null)
                _isMinimized.value = false
            }
            _dialogs.value = emptyList()
        }

        // endregion

        // region Guard and dialogs

        override fun attempt(action: RestrictedAction): Boolean {
            if (_session.value == null) return true
            enqueue(CallDialog.Guard(GuardMessage.EndCallTo(action)))
            return false
        }

        override fun attemptModifyBooking(
            bookingId: String,
            action: RestrictedAction,
        ): Boolean {
            val current = _session.value ?: return true
            if (current.bookingId != bookingId) return true
            enqueue(CallDialog.Guard(GuardMessage.SessionInProgress(action)))
            return false
        }

        /** Removes the dialog currently shown. */
        fun dismissDialog() {
            _dialogs.value = _dialogs.value.drop(1)
        }

        /** Appends [dialog] unless it equals the current tail, so repeated failures do not stack. */
        private fun enqueue(dialog: CallDialog) {
            val queue = _dialogs.value
            if (queue.lastOrNull() == dialog) return
            _dialogs.value = queue + dialog
        }

        // endregion

        private fun updateSession(
            id: Long,
            transform: (CallSession) -> CallSession,
        ) {
            val current = _session.value ?: return
            if (current.id != id) return
            setSession(transform(current))
        }

        /** Updates the session and the active flag together so [attempt] never reads a stale flag. */
        private fun setSession(value: CallSession?) {
            _session.value = value
            _isCallActive.value = value != null
        }

        companion object {
            /** Bound on `engine.leave()` during hang-up and logout teardown. */
            const val LEAVE_STEP_TIMEOUT_MS = 3_000L

            /** Bound on waiting for an in-flight confirm-connection before `/end`; `manual_exit` is sent after it. */
            const val CONFIRM_SETTLE_TIMEOUT_MS = 5_000L

            /** Bound on `POST /end`; teardown continues when it elapses. */
            const val END_REQUEST_TIMEOUT_MS = 10_000L

            /** Longest sleep between wall-clock checks of the overtime cutoff. */
            const val CUTOFF_POLL_MS = 30_000L
        }
    }
