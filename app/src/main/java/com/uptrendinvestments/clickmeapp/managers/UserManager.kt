package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.MeData
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

        /** Bumped by every logout so an in-flight profile fetch can tell its session is gone. */
        @Volatile
        private var sessionGeneration = 0

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
                // Phase 3 reconnects the realtime socket with the new token.
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
            val data = apiCall { api.login(LoginRequest(email, password)) }.data
            withContext(io) { tokens.save(TokenPair(data.token, data.refreshToken, data.expiresAt)) }
            _authUser.value = data.user
            _isLoggedIn.value = true
            _wasSuspended.value = false
            meStale = false
            // appScope, not the caller's scope: leaving the login screen must not cancel the refresh.
            appScope.launch { bestEffort { refreshProfile() } }
            return data.user
        }

        /**
         * The single logout path (FND-08, D-09): clears tokens, cached profile, mode and onboarding flag,
         * and resets in-memory state. No backend call: iOS has no logout endpoint. Runs non-cancellable so
         * a screen leaving composition cannot interrupt it half way.
         */
        suspend fun logout() {
            withContext(NonCancellable + io) {
                sessionGeneration++
                // Called first so the future push-token DELETE still has a bearer token (iOS order).
                runCatching { pushTokenRegistrar.unregister() }
                tokens.clear()
                _authUser.value = null
                _me.value = null
                _profile.value = null
                _currentMode.value = AppMode.Client
                _hasSeenOnboarding.value = false
                meStale = false
                sessionStore.clear()
                // AppViewModel maps this flip to the Login route (iOS onChange(of: isLoggedIn)).
                _isLoggedIn.value = false
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
            return resolvePostAuthDestination(roles)
        }

        private suspend fun loadPersistedState() {
            _currentMode.value = sessionStore.storedMode() ?: AppMode.Client
            _hasSeenOnboarding.value = sessionStore.hasSeenOnboarding()
            _profile.value = sessionStore.cachedProfile()
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
            return LaunchDestination.Home(_currentMode.value)
        }

        /**
         * Routes a freshly authenticated user (shared by [restore] and the login screen, D-07): an expert
         * with incomplete setup resumes signup (D-05); otherwise the stored mode, else expert only when the
         * expert has at least one topic. The chosen mode is persisted.
         */
        suspend fun resolvePostAuthDestination(roles: Set<UserRole>): LaunchDestination {
            val isExpert = UserRole.Expert in roles
            if (isExpert && isExpertProfileIncomplete()) return LaunchDestination.SignupSetup
            val mode =
                sessionStore.storedMode()
                    ?: if (isExpert && expertHasTopics()) AppMode.Expert else AppMode.Client
            setMode(mode)
            return LaunchDestination.Home(mode)
        }

        /** iOS: a failed lookup counts as complete, so a transient error never pushes users back into signup. */
        private suspend fun isExpertProfileIncomplete(): Boolean =
            try {
                !apiCall { api.getExpertProfile() }.data.setupCompleted
            } catch (e: ApiException) {
                false
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
                meStale = true
                LaunchDestination.Home(_currentMode.value)
            } else {
                LaunchDestination.Login
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
