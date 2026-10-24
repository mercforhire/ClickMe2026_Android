package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmConnectionRequest
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.time.Clock
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
 * Hang-up, the cutoff and logout teardown live in this class too (added by plan 09-11). The iOS `isRejoin`
 * parameter is obsolete after CALL-END-SEMANTICS and is not ported (RESEARCH Deprecated).
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
        CallStarter {
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

        init {
            scope.launch(Dispatchers.Main.immediate) { engine.events.collect(::onEngineEvent) }
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
            setSession(
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
                ),
            )
            _isMinimized.value = false
            startJob = scope.launch(Dispatchers.Main.immediate) { runStart(id, request) }
        }

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
            if (_session.value?.id != id) return
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
            if (_session.value?.id != id) return
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
            if (_session.value?.id != id) return
            if (!started) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown))
                return
            }
            val joined = engine.join(config.rtcToken, config.channelName, config.uid)
            if (_session.value?.id != id) return
            if (!joined) {
                abortStart(id, CallDialog.JoinError(JoinErrorReason.Unknown), engineTouched = true)
                return
            }
            val now = clock.instant()
            updateSession(id) { it.copy(joinStartedAt = now) }
        }

        /**
         * Tears the container down after a failed start. No `/end`: the user never joined a channel (D-04).
         * Does nothing when the session was already replaced or cleared.
         */
        private suspend fun abortStart(
            id: Long,
            dialog: CallDialog?,
            engineTouched: Boolean = false,
        ) {
            if (_session.value?.id != id) return
            if (engineTouched) engine.destroy()
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
         * Sends `PATCH confirm-connection` once per session. The flag is set before the request (iOS parity,
         * RESEARCH Pitfall 9).
         */
        private fun confirmConnectionOnce() {
            val current = _session.value ?: return
            if (current.confirmSent) return
            setSession(current.copy(confirmSent = true))
            val bookingId = current.bookingId
            val body =
                ConfirmConnectionRequest(
                    timestamp = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(),
                    method = current.connectionType.wire,
                )
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    apiCall { api.confirmCallConnection(bookingId, body) }
                } catch (_: ApiException) {
                    // 409 and every other error are ignored without interrupting the call (CALL-05).
                }
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
    }
