package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Session brain: login, auto-login ([restore]), logout, mode and onboarding persistence, the suspension
 * flag and the stale-`/me` retry. Ports iOS `UserManager` plus the launch routing of `SplashViewModel` and
 * the post-login routing of `LoginView`.
 *
 * Auth-failure policy (D-01, D-03): only transport-level rejection ends a session, and the transport
 * (Authenticator / SuspensionInterceptor) always clears the tokens synchronously before it does. So "the
 * store no longer has a session" is the signal this class acts on; anything else (IO, timeout, 5xx, a
 * 401/403 that left the tokens in place) keeps the session and routes from cached state.
 *
 * There is deliberately no timeout in here: the splash cap lives in AppViewModel and must never cancel
 * [restore] (a cancelled restore would leave state half-applied).
 */
@Singleton
class UserManager
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val tokens: SecureTokenStore,
        private val sessionStore: SessionStore,
        sessionEvents: SessionEvents,
        private val pushTokenRegistrar: PushTokenRegistrar,
        @ApplicationScope private val appScope: CoroutineScope,
        @IoDispatcher private val io: CoroutineDispatcher,
        private val sessionDataCleaners: Set<@JvmSuppressWildcards SessionDataCleaner> = emptySet(),
    ) {
        private val _isLoggedIn = MutableStateFlow(false)
        val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

        private val _wasSuspended = MutableStateFlow(false)
        val wasSuspended: StateFlow<Boolean> = _wasSuspended.asStateFlow()

        private val _currentMode = MutableStateFlow(AppMode.Client)
        val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

        private val _authUser = MutableStateFlow<AuthUser?>(null)
        val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

        private val _me = MutableStateFlow<MeData?>(null)
        val me: StateFlow<MeData?> = _me.asStateFlow()

        private val _profile = MutableStateFlow<UserProfileData?>(null)
        val profile: StateFlow<UserProfileData?> = _profile.asStateFlow()

        private val _hasSeenOnboarding = MutableStateFlow(false)
        val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

        /** True after a cached-path launch: `/me` and the profile still need a retry (D-01). */
        @Volatile
        private var meStale = false

        /**
         * True when a home route was chosen without a setup answer (D-11): the expert-profile lookup failed,
         * `/me` failed at launch, or the splash cap fired. Cleared only by a successful lookup inside
         * [recheckSetupIfPending], by the role guard there, and by login, signup, [completeSetup] and logout.
         * A lookup that succeeds during launch, login or a switch leaves it unchanged, so the late restore on
         * the cap path (whose SignupSetup result AppViewModel discards) cannot clear it.
         */
        @Volatile
        private var setupCheckPending = false

        /** True once [loadPersistedState] has run, so [offlineDestination] can load it when restore has not. */
        @Volatile
        private var persistedStateLoaded = false

        /** Bumped by every logout so an in-flight profile fetch or mode write can tell its session is gone. */
        @Volatile
        private var sessionGeneration = 0

        /**
         * Orders logout's [sessionGeneration] bump against [setModeForSession] (WR-01). A mode write that holds the
         * lock first finishes before logout clears the store; one that gets it after sees the new generation and
         * writes nothing, so a late switch or restore can never persist a mode into the next session.
         */
        private val modeWriteLock = Mutex()

        init {
            // The bus has no replay, so the subscription must exist before any request can emit.
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                sessionEvents.events.collect(::onSessionEvent)
            }
        }

        private suspend fun onSessionEvent(event: SessionEvent) {
            when (event) {
                SessionEvent.Rejected -> logout()
                SessionEvent.Suspended -> handleAccountSuspended()
                // RealtimeConnectionManager reconnects the realtime socket with the new token (RT-02).
                SessionEvent.Refreshed -> Unit
            }
        }

        // ---- login / logout ----

        /**
         * `POST /auth/login`. Persists the token pair, marks the user logged in and refreshes the cached
         * profile in the background. Email trimming is the caller's job (LoginViewModel), as on iOS.
         */
        suspend fun login(
            email: String,
            password: String,
        ): AuthUser {
            clearSessionData()
            val data = apiCall { api.login(LoginRequest(email, password)) }.data
            withContext(io) { tokens.save(TokenPair(data.token, data.refreshToken, data.expiresAt)) }
            _authUser.value = data.user
            _isLoggedIn.value = true
            _wasSuspended.value = false
            meStale = false
            // appScope, not the caller's scope: leaving the login screen must not cancel the refresh.
            setupCheckPending = false
            appScope.launch { bestEffort { refreshProfile() } }
            return data.user
        }

        /**
         * `POST /auth/signup` (D-01). Stores the token pair exactly as [login] does and marks the user logged
         * in, so the realtime manager picks up the session. Email trimming is the caller's job, as with login.
         */
        suspend fun signup(
            email: String,
            password: String,
        ): SignupData {
            clearSessionData()
            val data = apiCall { api.signup(SignupRequest(email, password)) }.data
            withContext(io) { tokens.save(TokenPair(data.token, data.refreshToken, data.expiresAt)) }
            _authUser.value = data.user
            _isLoggedIn.value = true
            _wasSuspended.value = false
            meStale = false
            setupCheckPending = false
            appScope.launch { bestEffort { refreshProfile() } }
            return data
        }

        /**
         * The single logout path (FND-08, D-09): clears tokens, cached profile, mode and onboarding flag,
         * session-local files ([SessionDataCleaner]) and resets in-memory state. No backend call: iOS has no logout endpoint. Runs non-cancellable so
         * a screen leaving composition cannot interrupt it half way.
         *
         * [_isLoggedIn] flips before the in-memory mode and onboarding flag are reset, so nothing reacting to
         * those can show client home or the carousel for a frame while the session is ending.
         */
        suspend fun logout() {
            withContext(NonCancellable + io) {
                modeWriteLock.withLock { sessionGeneration++ }
                // Called first so the future push-token DELETE still has a bearer token (iOS order).
                runCatching { pushTokenRegistrar.unregister() }
                tokens.clear()
                clearSessionData()
                _authUser.value = null
                _me.value = null
                _profile.value = null
                meStale = false
                setupCheckPending = false
                sessionStore.clear()
                // AppViewModel maps this flip to the Login route (iOS onChange(of: isLoggedIn)).
                _isLoggedIn.value = false
                _currentMode.value = AppMode.Client
                _hasSeenOnboarding.value = false
            }
        }

        /**
         * Runs every [SessionDataCleaner] (CR-03, T-04-18): on logout, and before login or signup so nothing a
         * previous session left on disk (for example after process death) can reach the next account. Best effort.
         */
        private suspend fun clearSessionData() {
            withContext(io) {
                sessionDataCleaners.forEach { cleaner -> bestEffort { cleaner.clearSessionData() } }
            }
        }

        /** Sets [wasSuspended] once and logs out; repeated events are ignored (iOS `guard !wasSuspended`). */
        private suspend fun handleAccountSuspended() {
            if (_wasSuspended.value) return
            _wasSuspended.value = true
            logout()
        }

        fun clearSuspensionFlag() {
            _wasSuspended.value = false
        }

        // ---- auto-login ----

        /**
         * Auto-login on launch (FND-07). Never throws and never times out; see the class KDoc.
         *
         * Cached state is loaded first, so the profile is populated even when `/me` then fails.
         */
        suspend fun restore(): LaunchDestination =
            try {
                restoreInternal()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                offlineDestination()
            }

        private suspend fun restoreInternal(): LaunchDestination {
            val generation = sessionGeneration
            loadPersistedState()
            if (!withContext(io) { tokens.hasSession() }) return LaunchDestination.Login
            _isLoggedIn.value = true
            try {
                refreshMe()
            } catch (e: ApiException) {
                return afterMeFailure(e)
            }
            appScope.launch { bestEffort { refreshProfile() } }
            val roles = _me.value?.userRoles ?: return offlineDestination()
            return resolvePostAuthDestination(roles, generation)
        }

        private suspend fun loadPersistedState() {
            _currentMode.value = sessionStore.storedMode() ?: AppMode.Client
            _hasSeenOnboarding.value = sessionStore.hasSeenOnboarding()
            _profile.value = sessionStore.cachedProfile()
            persistedStateLoaded = true
        }

        /**
         * Deliberate deviation from iOS `isAuthRejection` (D-01, D-03): iOS logs out on any `/me` 401 or
         * non-suspension 403. Android ends the session only when the transport cleared the tokens (refresh
         * rejected, missing refresh token, token-coded 401 after a refresh, ACCOUNT_SUSPENDED). A 401/403
         * that left the tokens in place is a soft refresh failure, so the session is kept and `/me` is
         * retried later (RESEARCH Pitfall 2).
         */
        private suspend fun afterMeFailure(error: ApiException): LaunchDestination {
            if (!withContext(io) { tokens.hasSession() }) {
                val suspended = error is ApiException.Http && error.code == ErrorCode.AccountSuspended
                if (suspended) handleAccountSuspended() else logout()
                return LaunchDestination.Login
            }
            meStale = true
            setupCheckPending = true
            return LaunchDestination.Home(_currentMode.value)
        }

        /**
         * Routes a freshly authenticated user (shared by [restore] and the login screen, D-07): an expert
         * with incomplete setup resumes signup (D-05); otherwise the stored mode, else expert only when the
         * expert has at least one topic. The chosen mode is persisted, but only while the session that asked is
         * still alive: when it ended during the lookups the answer is [LaunchDestination.Login] and nothing is
         * written (WR-01).
         */
        suspend fun resolvePostAuthDestination(roles: Set<UserRole>): LaunchDestination =
            resolvePostAuthDestination(roles, sessionGeneration)

        private suspend fun resolvePostAuthDestination(
            roles: Set<UserRole>,
            generation: Int,
        ): LaunchDestination {
            val isExpert = UserRole.Expert in roles
            if (isExpert && isExpertProfileIncomplete()) {
                return if (generation == sessionGeneration) LaunchDestination.SignupSetup else LaunchDestination.Login
            }
            val mode =
                sessionStore.storedMode()
                    ?: if (isExpert && expertHasTopics()) AppMode.Expert else AppMode.Client
            return if (setModeForSession(mode, generation)) LaunchDestination.Home(mode) else LaunchDestination.Login
        }

        /** `GET /expert/profile` `setup_completed`, or null when the lookup failed. */
        private suspend fun fetchSetupCompleted(): Boolean? =
            try {
                apiCall { api.getExpertProfile() }.data.setupCompleted
            } catch (e: ApiException) {
                null
            }

        /**
         * iOS: a failed lookup counts as complete, so a transient error never pushes users back into signup.
         * The failure marks the check pending so the next foreground retries it (D-11).
         */
        private suspend fun isExpertProfileIncomplete(): Boolean {
            val completed = fetchSetupCompleted()
            if (completed == null) {
                setupCheckPending = true
                return false
            }
            return !completed
        }

        private suspend fun expertHasTopics(): Boolean =
            try {
                apiCall { api.getMyTopics() }.data.topics.isNotEmpty()
            } catch (e: ApiException) {
                false
            }

        /**
         * Destination from in-memory state only, used when the splash cap fires before [restore] finishes
         * (D-02): Home(current mode) when a session exists, else Login. Marks `/me` stale.
         */
        suspend fun offlineDestination(): LaunchDestination =
            if (withContext(io) { tokens.hasSession() }) {
                // The cap can fire before restore loaded the persisted state; without it a returning user would
                // see the onboarding carousel flash (RESEARCH Pitfall 11).
                if (!persistedStateLoaded) loadPersistedState()
                meStale = true
                setupCheckPending = true
                LaunchDestination.Home(_currentMode.value)
            } else {
                LaunchDestination.Login
            }

        // ---- mode switch and setup gate ----

        /**
         * Mode switch (D-17). Client is always allowed and local only. Expert asks `GET /expert/profile`: an
         * incomplete setup returns [LaunchDestination.SignupSetup] without persisting Expert (a stored Expert
         * plus an incomplete profile would mislead the next launch); otherwise Expert is persisted. A failed
         * lookup counts as complete and marks the check pending.
         *
         * The GET goes through the backend's `ensureExpert` and permanently converts a client-only account to
         * expert. D-23 accepts this as the "become an expert" path, so there is no role pre-gate here.
         *
         * When the session ends while the switch runs (logout, rejection), nothing is persisted and the result is
         * [LaunchDestination.Login] (WR-01).
         */
        suspend fun requestModeSwitch(target: AppMode): LaunchDestination {
            val generation = sessionGeneration
            val destination =
                when (target) {
                    AppMode.Client -> LaunchDestination.Home(AppMode.Client)
                    AppMode.Expert ->
                        if (isExpertProfileIncomplete()) {
                            LaunchDestination.SignupSetup
                        } else {
                            LaunchDestination.Home(AppMode.Expert)
                        }
                }
            // WR-01: a switch whose session ended meanwhile writes nothing and reports Login.
            return when {
                destination is LaunchDestination.Home && !setModeForSession(destination.mode, generation) ->
                    LaunchDestination.Login
                generation != sessionGeneration -> LaunchDestination.Login
                else -> destination
            }
        }

        /**
         * Re-checks a setup check that could not run or whose answer was not applied (D-11). Order:
         * (a) nothing unless a check is pending and a session exists; (b) the roles must be known, from `/me`
         * else the login or signup user, otherwise stay pending (after an offline launch `/me` is loaded by
         * `refreshIfStale` on a later foreground); (c) role guard: without the expert role and outside Expert
         * mode there is nothing to check, so clear the flag with no request, because a background GET must
         * never trigger D-23's `ensureExpert` conversion for a client-only account (mode Expert means the
         * user explicitly switched, which D-23 accepts); (d) a failed lookup stays pending; (e) otherwise clear
         * the flag and report [LaunchDestination.SignupSetup] when setup is incomplete, else null.
         */
        suspend fun recheckSetupIfPending(): LaunchDestination? {
            if (!setupCheckPending || !withContext(io) { tokens.hasSession() }) return null
            val roles = _me.value?.userRoles ?: _authUser.value?.userRoles ?: return null
            if (UserRole.Expert !in roles && _currentMode.value != AppMode.Expert) {
                setupCheckPending = false
                return null
            }
            val completed = fetchSetupCompleted() ?: return null
            setupCheckPending = false
            return if (completed) null else LaunchDestination.SignupSetup
        }

        /**
         * Signup setup was published (D-22): land in Client mode and refresh the cached profile (new avatar). Does
         * nothing when the session has already ended (WR-01).
         */
        suspend fun completeSetup() {
            if (!setModeForSession(AppMode.Client, sessionGeneration)) return
            setupCheckPending = false
            appScope.launch { bestEffort { refreshProfile() } }
        }

        /** Re-fetches `/me` and the profile only after a cached-path launch (D-01 retry). */
        suspend fun refreshIfStale() {
            if (!meStale) return
            if (!withContext(io) { tokens.hasSession() }) return
            try {
                refreshMe()
                refreshProfile()
                meStale = false
            } catch (e: ApiException) {
                // Stay stale; the next foreground retries.
            }
        }

        // ---- data refresh ----

        suspend fun refreshMe() {
            _me.value = apiCall { api.getMe() }.data
        }

        /**
         * Fetches `/user/profile` and caches it, but only while the session that started the fetch is
         * still alive, so one user's profile is never written after a logout (T-02-37).
         */
        suspend fun refreshProfile() {
            val generation = sessionGeneration
            val fetched = apiCall { api.getUserProfile() }.data
            if (generation != sessionGeneration) return
            if (!withContext(io) { tokens.hasSession() }) return
            _profile.value = fetched
            sessionStore.setCachedProfile(fetched)
        }

        // ---- mode and onboarding ----

        suspend fun setMode(mode: AppMode) {
            _currentMode.value = mode
            sessionStore.setMode(mode)
        }

        /**
         * [setMode] for a result computed in session [generation] (WR-01): writes only while that session is still
         * the current one and still holds tokens, and returns whether it wrote. See [modeWriteLock].
         */
        private suspend fun setModeForSession(
            mode: AppMode,
            generation: Int,
        ): Boolean =
            modeWriteLock.withLock {
                if (generation != sessionGeneration || !withContext(io) { tokens.hasSession() }) return@withLock false
                setMode(mode)
                true
            }

        suspend fun hasStoredModePreference(): Boolean = sessionStore.storedMode() != null

        suspend fun markOnboardingShown() {
            sessionStore.setHasSeenOnboarding(true)
            _hasSeenOnboarding.value = true
        }

        // ---- debug ----

        /**
         * Replaces the access token with an invalid value (keeping the refresh token) and calls `/me`, so
         * the Authenticator's refresh path can be exercised on a device. Only reachable from debug-gated UI
         * (plan 02-13); it only degrades the caller's own session.
         */
        suspend fun debugForceTokenRefresh(): Result<Unit> {
            withContext(io) {
                val refresh = tokens.refreshToken
                if (refresh != null) {
                    tokens.save(TokenPair("debug-invalidated-access-token", refresh, tokens.expiresAt ?: 0L))
                }
            }
            return runCatching { refreshMe() }
        }

        /** Runs [block], swallowing everything except coroutine cancellation. */
        private suspend fun bestEffort(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // Best effort: the cached profile stays as it was.
            }
        }
    }
