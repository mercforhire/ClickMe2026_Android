@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoWorkingFilesCleaner
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakePhotoProcessor
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class UserManagerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val authedPair = TokenPair("access-1", "refresh-1", 1_900_000_000L)

    private val sampleProfile =
        UserProfileData(
            personalDetails =
                UserProfileData.PersonalDetails(
                    firstName = "Ada",
                    lastName = "Lovelace",
                    email = "a@b.co",
                ),
            professionalDetails = UserProfileData.ProfessionalDetails(jobTitle = "Engineer"),
            location = UserProfileData.Location(city = "London", country = "GBR"),
        )

    private val sampleLogin =
        LoginData(
            token = "new-access",
            user =
                AuthUser(
                    id = "u1",
                    firstName = "Ada",
                    lastName = "Lovelace",
                    roles = listOf("client"),
                ),
            refreshToken = "new-refresh",
            expiresAt = 1_900_000_100L,
            expiresIn = 3600L,
        )

    private fun me(vararg roles: String) = MeData(id = "u1", email = "a@b.co", roles = roles.toList())

    /** Registrar that records whether the access token was still present when unregister ran. */
    private class RecordingRegistrar(
        private val tokens: SecureTokenStore,
    ) : PushTokenRegistrar() {
        var calls = 0
        var tokenPresentAtCall: Boolean? = null

        override suspend fun unregister() {
            calls++
            tokenPresentAtCall = tokens.hasSession()
        }
    }

    /** Cleaner that records each call and whether a session was stored at that moment. */
    private class RecordingCleaner(
        private val tokens: SecureTokenStore,
    ) : SessionDataCleaner {
        var calls = 0
        val sessionPresentAtCall = mutableListOf<Boolean>()
        var failWith: Exception? = null

        override suspend fun clearSessionData() {
            calls++
            sessionPresentAtCall += tokens.hasSession()
            failWith?.let { throw it }
        }
    }

    private inner class Harness(
        scope: TestScope,
        initial: TokenPair? = null,
    ) {
        val api = FakeClickMeApi()
        val tokens = FakeSecureTokenStore(initial)
        val sessionStore = SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp))
        val events = SessionEvents()
        val registrar = RecordingRegistrar(tokens)
        val cleaner = RecordingCleaner(tokens)
        val manager =
            UserManager(
                api,
                tokens,
                sessionStore,
                events,
                registrar,
                scope.backgroundScope,
                UnconfinedTestDispatcher(scope.testScheduler),
                setOf(cleaner),
            )
    }

    // ---- Task 1: login, logout, mode, onboarding, events, suspension ----

    @Test
    fun loginPersistsTokensAndCachesProfile() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { sampleLogin }
            h.api.userProfileHandler = { sampleProfile }

            val user = h.manager.login("a@b.co", "pw")
            runCurrent()

            assertEquals("u1", user.id)
            assertEquals(TokenPair("new-access", "new-refresh", 1_900_000_100L).accessToken, h.tokens.accessToken)
            assertEquals("new-refresh", h.tokens.refreshToken)
            assertEquals(1_900_000_100L, h.tokens.expiresAt)
            assertEquals(user, h.manager.authUser.value)
            assertTrue(h.manager.isLoggedIn.value)
            assertFalse(h.manager.wasSuspended.value)
            assertEquals(sampleProfile, h.manager.profile.value)
            assertEquals(sampleProfile, h.sessionStore.cachedProfile())
        }

    @Test
    fun loginClearsSuspensionFlag() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { sampleLogin }
            h.api.userProfileHandler = { sampleProfile }
            h.events.emit(SessionEvent.Suspended)
            runCurrent()
            assertTrue(h.manager.wasSuspended.value)

            h.manager.login("a@b.co", "pw")
            runCurrent()

            assertFalse(h.manager.wasSuspended.value)
            assertTrue(h.manager.isLoggedIn.value)
        }

    @Test
    fun loginWithInvalidCredentialsThrowsHttp401AndStoresNothing() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { throw h.api.httpError(401, """{"message":"Invalid email or password"}""") }

            try {
                h.manager.login("a@b.co", "bad")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(401, e.status)
                assertEquals("Invalid email or password", e.serverMessage)
            }

            assertFalse(h.tokens.hasSession())
            assertFalse(h.manager.isLoggedIn.value)
            assertNull(h.manager.authUser.value)
        }

    @Test
    fun loginWithIoErrorThrowsNetwork() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { throw IOException("offline") }

            try {
                h.manager.login("a@b.co", "pw")
                fail("expected ApiException.Network")
            } catch (e: ApiException.Network) {
                assertNotNull(e.cause)
            }
            assertFalse(h.manager.isLoggedIn.value)
        }

    @Test
    fun logoutClearsEverythingAndUnregistersPushFirst() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }
            h.manager.restore()
            runCurrent()
            h.manager.setMode(AppMode.Expert)
            h.manager.markOnboardingShown()
            assertEquals(sampleProfile, h.manager.profile.value)

            h.manager.logout()

            assertEquals(1, h.registrar.calls)
            assertEquals(true, h.registrar.tokenPresentAtCall)
            assertEquals(1, h.tokens.clearCount)
            assertFalse(h.tokens.hasSession())
            assertNull(h.sessionStore.storedMode())
            assertFalse(h.sessionStore.hasSeenOnboarding())
            assertNull(h.sessionStore.cachedProfile())
            assertNull(h.manager.authUser.value)
            assertNull(h.manager.me.value)
            assertNull(h.manager.profile.value)
            assertEquals(AppMode.Client, h.manager.currentMode.value)
            assertFalse(h.manager.hasSeenOnboarding.value)
            assertFalse(h.manager.isLoggedIn.value)
        }

    @Test
    fun setModeExpertIsStoredAndLogoutRemovesPreference() =
        runTest {
            val h = Harness(this, authedPair)

            h.manager.setMode(AppMode.Expert)

            assertEquals(AppMode.Expert, h.manager.currentMode.value)
            assertTrue(h.manager.hasStoredModePreference())

            h.manager.logout()

            assertFalse(h.manager.hasStoredModePreference())
        }

    @Test
    fun markOnboardingShownIsPublishedAndPersisted() =
        runTest {
            val h = Harness(this)
            assertFalse(h.manager.hasSeenOnboarding.value)

            h.manager.markOnboardingShown()

            assertTrue(h.manager.hasSeenOnboarding.value)
            assertTrue(h.sessionStore.hasSeenOnboarding())
        }

    @Test
    fun rejectedEventLogsOut() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.sessionStore.setCachedProfile(sampleProfile)
            h.manager.restore()
            h.events.emit(SessionEvent.Rejected)
            runCurrent()

            assertFalse(h.manager.isLoggedIn.value)
            assertNull(h.sessionStore.storedMode())
            assertNull(h.sessionStore.cachedProfile())
            assertFalse(h.tokens.hasSession())
        }

    @Test
    fun suspendedEventSetsFlagOnceAndLogsOut() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)

            h.events.emit(SessionEvent.Suspended)
            runCurrent()

            assertTrue(h.manager.wasSuspended.value)
            assertFalse(h.manager.isLoggedIn.value)
            assertEquals(1, h.tokens.clearCount)
            assertNull(h.sessionStore.storedMode())

            h.events.emit(SessionEvent.Suspended)
            runCurrent()

            assertEquals("a second Suspended must be ignored", 1, h.tokens.clearCount)
            assertEquals(1, h.registrar.calls)

            h.manager.clearSuspensionFlag()
            assertFalse(h.manager.wasSuspended.value)
        }

    @Test
    fun profileRefreshFinishingAfterLogoutDoesNotWriteCache() =
        runTest {
            val h = Harness(this, authedPair)
            val gate = CompletableDeferred<UserProfileData>()
            h.api.userProfileHandler = { gate.await() }

            val refresh = backgroundScope.launch { runCatching { h.manager.refreshProfile() } }
            runCurrent()
            h.manager.logout()
            gate.complete(sampleProfile)
            runCurrent()

            assertTrue(refresh.isCompleted)
            assertNull(h.sessionStore.cachedProfile())
            assertNull(h.manager.profile.value)
        }

    @Test
    fun debugForceTokenRefreshInvalidatesAccessTokenAndCallsMe() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client") }

            val result = h.manager.debugForceTokenRefresh()

            assertTrue(result.isSuccess)
            assertEquals("debug-invalidated-access-token", h.tokens.accessToken)
            assertEquals("refresh-1", h.tokens.refreshToken)
            assertEquals(1, h.api.meCalls)
        }

    // ---- Task 2: restore matrix ----

    @Test
    fun restoreWithoutTokensReturnsLoginAndSkipsMe() =
        runTest {
            val h = Harness(this)

            assertEquals(LaunchDestination.Login, h.manager.restore())

            assertEquals(0, h.api.meCalls)
            assertFalse(h.manager.isLoggedIn.value)
        }

    @Test
    fun restoreClientOnlyWithoutStoredModeGoesHomeClient() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
            runCurrent()

            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertTrue(h.manager.isLoggedIn.value)
            assertEquals(sampleProfile, h.manager.profile.value)
            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun restoreExpertWithIncompleteSetupGoesToSignupSetup() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client", "expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.SignupSetup, h.manager.restore())
        }

    @Test
    fun restoreExpertWithTopicsAndNoStoredModeLandsInExpert() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client", "expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(mapOf("id" to "t1"))) }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
        }

    @Test
    fun restoreExpertWithoutTopicsLandsInClient() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
        }

    @Test
    fun restoreExpertWhenTopicsCallFailsLandsInClient() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { throw IOException("flaky") }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
        }

    @Test
    fun restoreExpertWhenExpertProfileCallFailsTreatsSetupAsComplete() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { throw IOException("flaky") }
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(mapOf("id" to "t1"))) }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())
        }

    @Test
    fun restoreHonorsStoredExpertModeForClientOnlyUser() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())
        }

    @Test
    fun restoreLoadsCachedStateBeforeNetworkEvenWhenMeFails() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setCachedProfile(sampleProfile)
            h.sessionStore.setMode(AppMode.Expert)
            h.sessionStore.setHasSeenOnboarding(true)
            h.api.meHandler = { throw IOException("offline") }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())

            assertEquals(sampleProfile, h.manager.profile.value)
            assertEquals(AppMode.Expert, h.manager.currentMode.value)
            assertTrue(h.manager.hasSeenOnboarding.value)
            assertTrue(h.tokens.hasSession())
        }

    @Test
    fun restoreKeepsSessionOnIoErrorAndRefreshIfStaleRetriesOnce() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { throw IOException("offline") }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
            assertTrue(h.manager.isLoggedIn.value)
            assertEquals(1, h.api.meCalls)

            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }
            h.manager.refreshIfStale()

            assertEquals(2, h.api.meCalls)
            assertEquals(1, h.api.userProfileCalls)
            assertNotNull(h.manager.me.value)
            assertEquals(sampleProfile, h.manager.profile.value)

            h.manager.refreshIfStale()

            assertEquals("stale flag must be cleared after a successful retry", 2, h.api.meCalls)
            assertEquals(1, h.api.userProfileCalls)
        }

    @Test
    fun restoreKeepsSessionOn503() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.api.meHandler = { throw h.api.httpError(503, """{"message":"down"}""") }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())

            assertTrue(h.tokens.hasSession())
            assertEquals(0, h.tokens.clearCount)
        }

    @Test
    fun restoreKeepsSessionWhenTokensStillPresentAfterMe401() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.sessionStore.setCachedProfile(sampleProfile)
            // Soft refresh failure: the transport saw the 401 but left the tokens in place.
            h.api.meHandler = { throw h.api.httpError(401, """{"message":"Unauthorized"}""") }

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.restore())
            runCurrent()

            assertTrue("tokens still present after a /me 401", h.tokens.hasSession())
            assertEquals(authedPair.accessToken, h.tokens.accessToken)
            assertEquals(0, h.tokens.clearCount)
            assertTrue(h.manager.isLoggedIn.value)
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
            assertEquals(sampleProfile, h.sessionStore.cachedProfile())
        }

    @Test
    fun restoreReturnsLoginWhenTransportRejectedSession() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.sessionStore.setCachedProfile(sampleProfile)
            h.api.meHandler = {
                h.tokens.clear()
                h.events.emit(SessionEvent.Rejected)
                throw h.api.httpError(401, """{"code":"TOKEN_EXPIRED"}""")
            }

            assertEquals(LaunchDestination.Login, h.manager.restore())
            runCurrent()

            assertFalse(h.manager.isLoggedIn.value)
            assertNull(h.sessionStore.storedMode())
            assertNull(h.sessionStore.cachedProfile())
            assertNull(h.manager.profile.value)
        }

    @Test
    fun restoreReturnsLoginAndFlagsSuspensionWhenAccountSuspended() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setMode(AppMode.Expert)
            h.api.meHandler = {
                h.tokens.clear()
                h.events.emit(SessionEvent.Suspended)
                throw h.api.httpError(403, """{"code":"${ErrorCode.AccountSuspended.wire}"}""")
            }

            assertEquals(LaunchDestination.Login, h.manager.restore())
            runCurrent()

            assertTrue(h.manager.wasSuspended.value)
            assertFalse(h.manager.isLoggedIn.value)
            assertNull(h.sessionStore.storedMode())
            assertEquals("suspension flag is single-shot", 1, h.registrar.calls)
        }

    @Test
    fun offlineDestinationWithTokensIsHomeOfCurrentModeAndMarksStale() =
        runTest {
            val h = Harness(this, authedPair)
            h.manager.setMode(AppMode.Expert)

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.offlineDestination())

            h.api.meHandler = { me("client", "expert") }
            h.api.userProfileHandler = { sampleProfile }
            h.manager.refreshIfStale()
            assertEquals(1, h.api.meCalls)
        }

    @Test
    fun offlineDestinationWithoutTokensIsLogin() =
        runTest {
            val h = Harness(this)

            assertEquals(LaunchDestination.Login, h.manager.offlineDestination())
        }

    @Test
    fun refreshIfStaleDoesNothingWhenNotStale() =
        runTest {
            val h = Harness(this, authedPair)

            h.manager.refreshIfStale()

            assertEquals(0, h.api.meCalls)
            assertEquals(0, h.api.userProfileCalls)
        }

    @Test
    fun restoreMapsUnexpectedFailureToOfflineDestination() =
        runTest {
            val h = Harness(this, authedPair)
            // An exception outside ApiException must still never escape restore().
            h.api.meHandler = { throw IllegalStateException("boom") }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
            assertTrue(h.tokens.hasSession())
        }

    @Test
    fun resolvePostAuthDestinationForFreshClientLoginPersistsMode() =
        runTest {
            val h = Harness(this, authedPair)

            val destination = h.manager.resolvePostAuthDestination(setOf(UserRole.Client))

            assertEquals(LaunchDestination.Home(AppMode.Client), destination)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertEquals(AppMode.Client, h.manager.currentMode.value)
        }

    // ---- Phase 4: signup, mode switch, setup re-check ----

    private val sampleSignup =
        SignupData(
            token = "s-access",
            user = AuthUser(id = "u2", firstName = "Bo", lastName = "Ng", roles = listOf("expert")),
            refreshToken = "s-refresh",
            expiresAt = 1_900_000_200L,
            expiresIn = 3600L,
            emailVerified = true,
        )

    private fun expertLogin() = sampleLogin.copy(user = sampleLogin.user.copy(roles = listOf("expert")))

    @Test
    fun signupStoresTokensAndMarksLoggedIn() =
        runTest {
            val h = Harness(this)
            h.api.signupHandler = { sampleSignup }
            h.api.userProfileHandler = { sampleProfile }

            val data = h.manager.signup("a@b.co", "abcdefg1")
            runCurrent()

            assertEquals(sampleSignup, data)
            assertTrue(data.emailVerified)
            assertEquals(1, h.api.signupCalls)
            assertEquals("s-access", h.tokens.accessToken)
            assertEquals("s-refresh", h.tokens.refreshToken)
            assertEquals(1_900_000_200L, h.tokens.expiresAt)
            assertEquals(sampleSignup.user, h.manager.authUser.value)
            assertTrue(h.manager.isLoggedIn.value)
            assertFalse(h.manager.wasSuspended.value)
        }

    @Test
    fun signupSendsExpertRole() =
        runTest {
            val h = Harness(this)
            var sent: SignupRequest? = null
            h.api.signupHandler = {
                sent = it
                sampleSignup
            }
            h.api.userProfileHandler = { sampleProfile }

            h.manager.signup("a@b.co", "abcdefg1")

            assertEquals("a@b.co", sent?.email)
            assertEquals("abcdefg1", sent?.password)
            assertEquals("expert", sent?.role)
        }

    @Test
    fun signupEmailTakenThrowsHttp409AndStoresNothing() =
        runTest {
            val h = Harness(this)
            h.api.signupHandler = { throw h.api.httpError(409, """{"code":"EMAIL_TAKEN"}""") }

            try {
                h.manager.signup("a@b.co", "abcdefg1")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(409, e.status)
            }

            assertFalse(h.tokens.hasSession())
            assertFalse(h.manager.isLoggedIn.value)
        }

    @Test
    fun requestModeSwitchClientPersistsWithoutServerCall() =
        runTest {
            val h = Harness(this, authedPair)
            h.manager.setMode(AppMode.Expert)

            val destination = h.manager.requestModeSwitch(AppMode.Client)

            assertEquals(LaunchDestination.Home(AppMode.Client), destination)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun requestModeSwitchExpertWithCompleteSetupPersistsExpert() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }

            val destination = h.manager.requestModeSwitch(AppMode.Expert)

            assertEquals(LaunchDestination.Home(AppMode.Expert), destination)
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
            assertEquals(AppMode.Expert, h.manager.currentMode.value)
        }

    @Test
    fun requestModeSwitchExpertWithIncompleteSetupDoesNotPersistExpert() =
        runTest {
            val h = Harness(this, authedPair)
            h.manager.setMode(AppMode.Client)
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }

            val destination = h.manager.requestModeSwitch(AppMode.Expert)

            assertEquals(LaunchDestination.SignupSetup, destination)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertEquals(AppMode.Client, h.manager.currentMode.value)
        }

    @Test
    fun requestModeSwitchExpertWithFailedLookupCountsAsCompleteAndMarksPending() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.expertProfileHandler = { throw IOException("offline") }

            val destination = h.manager.requestModeSwitch(AppMode.Expert)

            assertEquals(LaunchDestination.Home(AppMode.Expert), destination)
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
            // Pending is observable through the re-check once the roles are known.
            h.api.meHandler = { me("client") }
            h.manager.refreshMe()
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())
        }

    @Test
    fun recheckWithNothingPendingMakesNoCall() =
        runTest {
            val h = Harness(this, authedPair)

            assertNull(h.manager.recheckSetupIfPending())

            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun recheckAfterFailedLoginLookupFindsIncompleteSetup() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { expertLogin() }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            val user = h.manager.login("a@b.co", "pw")
            assertEquals(
                LaunchDestination.Home(AppMode.Client),
                h.manager.resolvePostAuthDestination(user.userRoles),
            )
            assertEquals(1, h.api.expertProfileCalls)

            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())
            assertEquals(2, h.api.expertProfileCalls)
        }

    @Test
    fun recheckWithCompleteSetupClearsPending() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { expertLogin() }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            val user = h.manager.login("a@b.co", "pw")
            h.manager.resolvePostAuthDestination(user.userRoles)

            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(2, h.api.expertProfileCalls)
            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(2, h.api.expertProfileCalls)
        }

    @Test
    fun recheckThatKeepsFailingStaysPending() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { expertLogin() }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            val user = h.manager.login("a@b.co", "pw")
            h.manager.resolvePostAuthDestination(user.userRoles)

            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(2, h.api.expertProfileCalls)

            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())
        }

    @Test
    fun meFailureMarksPendingAndRecheckWaitsForRoles() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { throw IOException("offline") }

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.restore())
            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(0, h.api.expertProfileCalls)

            h.api.meHandler = { me("expert") }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            h.manager.refreshIfStale()

            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())
        }

    @Test
    fun recheckRoleGuardSkipsClientOnlyAccountAndClearsFlag() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { throw IOException("offline") }
            h.manager.restore()
            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }
            h.manager.refreshIfStale()

            assertNull(h.manager.recheckSetupIfPending())
            assertNull(h.manager.recheckSetupIfPending())

            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun capPathLateSignupSetupResultKeepsFlagForRecheck() =
        runTest {
            val h = Harness(this, authedPair)
            val gate = CompletableDeferred<Unit>()
            h.sessionStore.setHasSeenOnboarding(true)
            h.api.meHandler = {
                gate.await()
                me("expert")
            }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            var restored: LaunchDestination? = null
            backgroundScope.launch { restored = h.manager.restore() }
            runCurrent()

            assertEquals(LaunchDestination.Home(AppMode.Client), h.manager.offlineDestination())
            gate.complete(Unit)
            runCurrent()

            assertEquals(LaunchDestination.SignupSetup, restored)
            val before = h.api.expertProfileCalls
            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())
            assertEquals(before + 1, h.api.expertProfileCalls)
        }

    @Test
    fun recheckAfterExpertSwitchWithFailedLookupPassesRoleGuardForClientRoles() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("client") }
            h.api.userProfileHandler = { sampleProfile }
            h.manager.restore()
            h.api.expertProfileHandler = { throw IOException("offline") }
            assertEquals(LaunchDestination.Home(AppMode.Expert), h.manager.requestModeSwitch(AppMode.Expert))
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
            val before = h.api.expertProfileCalls

            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            assertEquals(LaunchDestination.SignupSetup, h.manager.recheckSetupIfPending())

            assertEquals(before + 1, h.api.expertProfileCalls)
        }

    @Test
    fun completeSetupPersistsClientAndClearsPending() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.userProfileHandler = { sampleProfile }
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.manager.refreshMe()
            h.manager.requestModeSwitch(AppMode.Expert)

            h.manager.completeSetup()
            runCurrent()

            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertEquals(AppMode.Client, h.manager.currentMode.value)
            val before = h.api.expertProfileCalls
            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(before, h.api.expertProfileCalls)
            assertEquals(sampleProfile, h.manager.profile.value)
        }

    @Test
    fun logoutClearsPending() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("expert") }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.manager.refreshMe()
            h.manager.requestModeSwitch(AppMode.Expert)
            h.manager.logout()

            h.tokens.save(authedPair)
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            h.manager.restore()
            val before = h.api.expertProfileCalls
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }

            assertNull(h.manager.recheckSetupIfPending())
            assertEquals(before, h.api.expertProfileCalls)
        }

    @Test
    fun logoutFlipsLoggedInBeforeModeReset() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = { me("expert") }
            h.api.userProfileHandler = { sampleProfile }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            h.manager.restore()
            h.manager.setMode(AppMode.Expert)
            h.manager.markOnboardingShown()
            val pairs = mutableListOf<Pair<Boolean, AppMode>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                combine(h.manager.isLoggedIn, h.manager.currentMode) { l, m -> l to m }.collect { pairs += it }
            }
            runCurrent()
            assertEquals(true to AppMode.Expert, pairs.last())

            h.manager.logout()
            runCurrent()

            assertFalse(pairs.contains(true to AppMode.Client))
            assertEquals(false to AppMode.Client, pairs.last())
        }

    // ---- CR-03: session-local files ----

    @Test
    fun logoutClearsSessionDataAfterTheTokens() =
        runTest {
            val h = Harness(this, authedPair)

            h.manager.logout()

            assertEquals(1, h.cleaner.calls)
            assertEquals(listOf(false), h.cleaner.sessionPresentAtCall)
        }

    @Test
    fun rejectedSessionDuringRestoreClearsSessionData() =
        runTest {
            val h = Harness(this, authedPair)
            h.api.meHandler = {
                h.tokens.clear()
                throw h.api.httpError(401, """{"code":"TOKEN_EXPIRED"}""")
            }

            assertEquals(LaunchDestination.Login, h.manager.restore())

            assertEquals(1, h.cleaner.calls)
        }

    @Test
    fun loginAndSignupClearSessionDataBeforeStoringTokens() =
        runTest {
            val h = Harness(this)
            h.api.loginHandler = { sampleLogin }
            h.api.signupHandler = { sampleSignup }
            h.api.userProfileHandler = { sampleProfile }

            h.manager.login("a@b.co", "pw")
            h.tokens.clear()
            h.manager.signup("b@b.co", "abcdefg1")

            assertEquals(2, h.cleaner.calls)
            assertEquals(listOf(false, false), h.cleaner.sessionPresentAtCall)
        }

    @Test
    fun logoutWithNoSetupRepositoryInTheProcessStillDeletesPhotoWorkingFiles() =
        runTest {
            val photo = FakePhotoProcessor()
            photo.cropped = byteArrayOf(1, 2, 3)
            photo.hasSourceValue = true
            val tokens = FakeSecureTokenStore(authedPair)
            val manager =
                UserManager(
                    FakeClickMeApi(),
                    tokens,
                    SessionStore(testPreferencesDataStore(backgroundScope, tmp)),
                    SessionEvents(),
                    PushTokenRegistrar(),
                    backgroundScope,
                    UnconfinedTestDispatcher(testScheduler),
                    setOf(PhotoWorkingFilesCleaner(photo)),
                )

            manager.logout()

            assertEquals(1, photo.clearCalls)
            assertNull(photo.croppedJpeg())
            assertFalse(photo.hasSource())
        }

    @Test
    fun failingCleanerDoesNotBreakLogoutOrLogin() =
        runTest {
            val h = Harness(this, authedPair)
            h.cleaner.failWith = IOException("disk")
            h.api.loginHandler = { sampleLogin }
            h.api.userProfileHandler = { sampleProfile }

            h.manager.logout()
            assertFalse(h.manager.isLoggedIn.value)
            assertFalse(h.tokens.hasSession())

            h.manager.login("a@b.co", "pw")
            assertTrue(h.manager.isLoggedIn.value)
            assertEquals(2, h.cleaner.calls)
        }

    @Test
    fun offlineDestinationLoadsPersistedOnboardingFlag() =
        runTest {
            val h = Harness(this, authedPair)
            h.sessionStore.setHasSeenOnboarding(true)
            assertFalse(h.manager.hasSeenOnboarding.value)

            h.manager.offlineDestination()

            assertTrue(h.manager.hasSeenOnboarding.value)
        }
}
