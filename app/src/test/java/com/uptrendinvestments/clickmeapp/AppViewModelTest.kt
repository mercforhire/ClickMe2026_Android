@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp

import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import com.uptrendinvestments.clickmeapp.testing.topicItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class AppViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val authedPair = TokenPair("access-1", "refresh-1", 1_900_000_000L)

    private fun me(vararg roles: String) = MeData(id = "u1", email = "a@b.co", roles = roles.toList())

    /** A real [UserManager] over fakes; the ViewModel is created after the test has scripted the API. */
    private inner class Harness(
        scope: TestScope,
        initial: TokenPair? = authedPair,
    ) {
        val api = FakeClickMeApi()
        val guard = FakeCallGuard()
        val tokens = FakeSecureTokenStore(initial)
        val sessionStore = SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp))
        val events = SessionEvents()
        val manager =
            UserManager(
                api,
                tokens,
                sessionStore,
                events,
                PushTokenRegistrar(),
                scope.backgroundScope,
                main.testDispatcher,
            )

        init {
            // The background profile refresh is best effort; fail it quietly.
            api.userProfileHandler = { throw IOException("not needed") }
        }

        fun viewModel() = AppViewModel(manager, guard)
    }

    private fun Harness.clientLaunch() {
        api.meHandler = { me("client") }
    }

    // ---- launch routing ----

    @Test
    fun noTokensRoutesToLoginAndReleasesSplashWithoutWaiting() =
        runTest(main.testDispatcher) {
            val h = Harness(this, initial = null)
            val vm = h.viewModel()

            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertTrue(vm.destinationResolved.value)
            assertEquals(0, h.api.meCalls)
        }

    @Test
    fun clientWithTokensRoutesToClientHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()

            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertTrue(vm.destinationResolved.value)
        }

    @Test
    fun expertWithTopicsAndNoStoredModeRoutesToExpertHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { me("client", "expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(topicItem())) }
            val vm = h.viewModel()

            runCurrent()

            assertEquals(AppRoute.ExpertHome, vm.route.value)
        }

    @Test
    fun expertWithUnfinishedSetupRoutesToSignupSetup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            val vm = h.viewModel()

            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
            assertTrue(vm.destinationResolved.value)
        }

    // ---- the 5 s splash cap ----

    @Test
    fun hangingMeReleasesSplashAtCapToCachedClientHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<Unit>()
            var cancelled = false
            h.api.meHandler = {
                try {
                    gate.await()
                } catch (e: CancellationException) {
                    cancelled = true
                    throw e
                }
                me("client")
            }
            val vm = h.viewModel()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS - 1)
            runCurrent()
            assertEquals(AppRoute.Splash, vm.route.value)
            assertFalse(vm.destinationResolved.value)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertTrue(vm.destinationResolved.value)
            assertFalse("the cap must not cancel the in-flight /me", cancelled)

            gate.complete(Unit)
            runCurrent()
            assertFalse(cancelled)
            assertEquals(AppRoute.ClientHome, vm.route.value)
        }

    @Test
    fun hangingMeWithStoredExpertModeReleasesToExpertHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.sessionStore.setMode(AppMode.Expert)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                me("client")
            }
            val vm = h.viewModel()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS + 1)
            runCurrent()

            assertEquals(AppRoute.ExpertHome, vm.route.value)
            assertTrue(vm.destinationResolved.value)
            gate.complete(Unit)
            runCurrent()
        }

    @Test
    fun rejectionAfterCapSwitchesCachedHomeToLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                h.tokens.clear()
                h.events.emit(SessionEvent.Rejected)
                throw h.api.httpError(401, """{"code":"TOKEN_EXPIRED"}""")
            }
            val vm = h.viewModel()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS + 1)
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            gate.complete(Unit)
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
        }

    // ---- logout and suspension ----

    @Test
    fun logoutFromClientHomeRoutesToLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            vm.logout()
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertFalse(h.manager.isLoggedIn.value)
        }

    @Test
    fun logoutDuringACallIsBlockedAndKeepsTheSession() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            h.guard.blocked = true

            vm.logout()
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertTrue(h.manager.isLoggedIn.value)
            assertEquals(listOf(RestrictedAction.LogOut), h.guard.attempts)
        }

    @Test
    fun forcedLogoutIsNotGuardedDuringACall() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            h.guard.blocked = true

            h.events.emit(SessionEvent.Rejected)
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertFalse(h.manager.isLoggedIn.value)
            assertTrue(h.guard.attempts.isEmpty())
        }

    @Test
    fun logoutFromSignupSetupRoutesToLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { me("expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.SignupSetup, vm.route.value)

            vm.logout()
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
        }

    @Test
    fun suspensionMidSessionRoutesToLoginAndShowsDialogUntilDismissed() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.sessionStore.setMode(AppMode.Expert)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ExpertHome, vm.route.value)
            assertFalse(vm.showSuspendedDialog.value)

            h.tokens.clear()
            h.events.emit(SessionEvent.Suspended)
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertTrue(vm.showSuspendedDialog.value)

            vm.onSuspendedDialogDismissed()
            runCurrent()

            assertFalse(vm.showSuspendedDialog.value)
            assertEquals(AppRoute.Login, vm.route.value)
        }

    @Test
    fun suspensionAtLaunchShowsDialogOverLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = {
                h.tokens.clear()
                h.events.emit(SessionEvent.Suspended)
                throw h.api.httpError(403, """{"code":"${ErrorCode.AccountSuspended.wire}"}""")
            }
            val vm = h.viewModel()

            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertTrue(vm.destinationResolved.value)
            assertTrue(vm.showSuspendedDialog.value)
        }

    // ---- login hand-off ----

    @Test
    fun onLoggedInMapsEveryDestinationToItsRoute() =
        runTest(main.testDispatcher) {
            val h = Harness(this, initial = null)
            val vm = h.viewModel()
            runCurrent()

            vm.onLoggedIn(LaunchDestination.Home(AppMode.Expert))
            assertEquals(AppRoute.ExpertHome, vm.route.value)

            vm.onLoggedIn(LaunchDestination.Home(AppMode.Client))
            assertEquals(AppRoute.ClientHome, vm.route.value)

            vm.onLoggedIn(LaunchDestination.SignupSetup)
            assertEquals(AppRoute.SignupSetup, vm.route.value)

            vm.onLoggedIn(LaunchDestination.Login)
            assertEquals(AppRoute.Login, vm.route.value)
        }

    // ---- foreground retry ----

    @Test
    fun foregroundAfterCachedPathLaunchRetriesMe() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { throw IOException("offline") }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(1, h.api.meCalls)

            h.api.meHandler = { me("client") }
            vm.onAppForegrounded()
            runCurrent()

            assertEquals(2, h.api.meCalls)
        }

    @Test
    fun foregroundAfterNormalLaunchDoesNotCallMe() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(1, h.api.meCalls)

            vm.onAppForegrounded()
            runCurrent()

            assertEquals(1, h.api.meCalls)
        }

    // ---- route order ----

    @Test
    fun appRouteMirrorsIosRoutesInOrder() {
        assertEquals(
            listOf(
                AppRoute.Splash,
                AppRoute.Login,
                AppRoute.SignupSetup,
                AppRoute.ClientHome,
                AppRoute.ExpertHome,
            ),
            AppRoute.entries,
        )
    }

    // ---- Phase 4: mode switch, follow-the-mode, foreground setup re-check, publish ----

    private fun Harness.expertLaunch(
        setupCompleted: Boolean = true,
        lookupFails: Boolean = false,
    ) {
        api.meHandler = { me("expert") }
        api.expertProfileHandler = {
            if (lookupFails) throw IOException("offline")
            ExpertProfileData(setupCompleted = setupCompleted)
        }
        api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
    }

    private fun TestScope.recordRoutes(vm: AppViewModel): List<AppRoute> {
        val routes = mutableListOf<AppRoute>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.route.toList(routes) }
        return routes
    }

    @Test
    fun switchModeToExpertWithCompleteSetupRoutesToExpertHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            val vm = h.viewModel()
            runCurrent()

            vm.switchMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.ExpertHome, vm.route.value)
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())
        }

    @Test
    fun switchModeDuringACallIsBlockedAndDoesNotSwitch() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            val vm = h.viewModel()
            runCurrent()
            h.guard.blocked = true

            vm.switchMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(AppMode.Client, h.manager.currentMode.value)
            assertEquals(listOf(RestrictedAction.SwitchModes), h.guard.attempts)
        }

    @Test
    fun switchModeToExpertWithIncompleteSetupRoutesToSignupSetupWithoutPersisting() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            val vm = h.viewModel()
            runCurrent()

            vm.switchMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
        }

    @Test
    fun switchModeToExpertWithFailedLookupRoutesToExpertHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.api.expertProfileHandler = { throw IOException("offline") }
            val vm = h.viewModel()
            runCurrent()

            vm.switchMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.ExpertHome, vm.route.value)
        }

    @Test
    fun switchModeToClientFromExpertHomeMakesNoServerCall() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.sessionStore.setMode(AppMode.Expert)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ExpertHome, vm.route.value)

            vm.switchMode(AppMode.Client)
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun quickSecondSwitchCancelsTheFirst() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val gate = CompletableDeferred<Unit>()
            h.api.expertProfileHandler = {
                gate.await()
                ExpertProfileData(setupCompleted = true)
            }
            val vm = h.viewModel()
            runCurrent()

            vm.switchMode(AppMode.Expert)
            runCurrent()
            vm.switchMode(AppMode.Client)
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
        }

    @Test
    fun switchCompletingAfterLogoutLeavesRouteOnLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val gate = CompletableDeferred<Unit>()
            h.api.expertProfileHandler = {
                gate.await()
                ExpertProfileData(setupCompleted = true)
            }
            val vm = h.viewModel()
            runCurrent()

            vm.switchMode(AppMode.Expert)
            runCurrent()
            vm.logout()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertEquals(null, h.sessionStore.storedMode())
        }

    @Test
    fun currentModeChangeMovesRouteOnlyOnHomeRoutes() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            h.manager.setMode(AppMode.Expert)
            runCurrent()
            assertEquals(AppRoute.ExpertHome, vm.route.value)

            h.manager.setMode(AppMode.Client)
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)
        }

    @Test
    fun currentModeChangeIgnoredOnLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this, initial = null)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.Login, vm.route.value)

            h.manager.setMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
        }

    @Test
    fun currentModeChangeIgnoredOnSignupSetup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(setupCompleted = false)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.SignupSetup, vm.route.value)

            h.manager.setMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
        }

    @Test
    fun logoutFromExpertHomeNeverShowsClientHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            h.sessionStore.setMode(AppMode.Expert)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ExpertHome, vm.route.value)
            val routes = recordRoutes(vm)
            runCurrent()

            vm.logout()
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertFalse(routes.contains(AppRoute.ClientHome))
            assertEquals(AppRoute.Login, routes.last())
        }

    @Test
    fun foregroundAfterFailedExpertLookupMovesHomeToSignupSetup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(lookupFails = true)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            h.expertLaunch(setupCompleted = false)
            vm.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
        }

    @Test
    fun foregroundAfterMeFailureLoadsMeThenMovesExpertToSignupSetup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { throw IOException("offline") }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            h.expertLaunch(setupCompleted = false)
            vm.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
        }

    @Test
    fun foregroundForClientOnlyAccountNeverCallsExpertProfile() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.meHandler = { throw IOException("offline") }
            val vm = h.viewModel()
            runCurrent()

            h.clientLaunch()
            vm.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(0, h.api.expertProfileCalls)
        }

    @Test
    fun capPathExpertSetupIncompleteAppliesOnNextForeground() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(setupCompleted = false)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                me("expert")
            }
            val vm = h.viewModel()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS + 1)
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)
            gate.complete(Unit)
            runCurrent()
            assertEquals("late SignupSetup result is discarded", AppRoute.ClientHome, vm.route.value)

            vm.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
        }

    @Test
    fun foregroundOnSignupSetupRouteMakesNoFurtherLookup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(lookupFails = true)
            val vm = h.viewModel()
            runCurrent()
            h.expertLaunch(setupCompleted = false)
            vm.switchMode(AppMode.Expert)
            runCurrent()
            assertEquals(AppRoute.SignupSetup, vm.route.value)
            val before = h.api.expertProfileCalls

            vm.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
            assertEquals(before, h.api.expertProfileCalls)
        }

    @Test
    fun expertModeSurvivesRelaunch() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(setupCompleted = true)
            val first = h.viewModel()
            runCurrent()
            first.switchMode(AppMode.Expert)
            runCurrent()
            assertEquals(AppRoute.ExpertHome, first.route.value)
            assertEquals(AppMode.Expert, h.sessionStore.storedMode())

            val relaunched =
                UserManager(
                    h.api,
                    h.tokens,
                    h.sessionStore,
                    h.events,
                    PushTokenRegistrar(),
                    backgroundScope,
                    main.testDispatcher,
                )
            val second = AppViewModel(relaunched, h.guard)
            runCurrent()

            assertEquals(AppRoute.ExpertHome, second.route.value)
        }

    @Test
    fun onSetupPublishedRoutesToClientHomeAndPersistsClient() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(setupCompleted = false)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.SignupSetup, vm.route.value)

            vm.onSetupPublished()
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertEquals(AppMode.Client, h.manager.currentMode.value)
        }

    @Test
    fun onSetupPublishedAfterTheSessionEndedStaysOnLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.expertLaunch(setupCompleted = false)
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.SignupSetup, vm.route.value)
            h.events.emit(SessionEvent.Rejected)
            runCurrent()
            assertEquals(AppRoute.Login, vm.route.value)

            vm.onSetupPublished()
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertEquals(null, h.sessionStore.storedMode())
        }

    // ---- CR-01 / D-27: "Not now" on the setup hub; roles match the backend's ['client','expert'] ----

    private fun Harness.convertedClientWithUnfinishedSetup() {
        api.meHandler = { me("client", "expert") }
        api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
        api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
    }

    private fun TestScope.relaunch(h: Harness): AppViewModel =
        AppViewModel(
            UserManager(
                h.api,
                h.tokens,
                h.sessionStore,
                h.events,
                PushTokenRegistrar(),
                backgroundScope,
                main.testDispatcher,
            ),
            h.guard,
        )

    /** Launches into the unfinished hub and taps "Not now". */
    private fun TestScope.launchAndDefer(h: Harness): AppViewModel {
        h.convertedClientWithUnfinishedSetup()
        val vm = h.viewModel()
        runCurrent()
        assertEquals(AppRoute.SignupSetup, vm.route.value)
        vm.onSetupDeferred()
        runCurrent()
        return vm
    }

    @Test
    fun notNowRoutesToClientHomeAndRemembersTheDeferral() =
        runTest(main.testDispatcher) {
            val h = Harness(this)

            val vm = launchAndDefer(h)

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertEquals(AppMode.Client, h.sessionStore.storedMode())
            assertTrue(h.sessionStore.isSetupDeferred())
        }

    @Test
    fun relaunchWhileDeferredGoesToClientHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            launchAndDefer(h)
            val before = h.api.expertProfileCalls

            val relaunched = relaunch(h)
            runCurrent()

            assertEquals(AppRoute.ClientHome, relaunched.route.value)
            assertEquals(before, h.api.expertProfileCalls)
        }

    @Test
    fun foregroundRecheckIsSkippedWhileDeferred() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            launchAndDefer(h)
            h.api.meHandler = { throw IOException("offline") }
            val relaunched = relaunch(h)
            runCurrent()
            assertEquals(AppRoute.ClientHome, relaunched.route.value)
            h.convertedClientWithUnfinishedSetup()
            val before = h.api.expertProfileCalls

            relaunched.onAppForegrounded()
            runCurrent()

            assertEquals(AppRoute.ClientHome, relaunched.route.value)
            assertEquals(before, h.api.expertProfileCalls)
        }

    @Test
    fun switchingToExpertReopensSetupAndClearsTheDeferral() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val vm = launchAndDefer(h)

            vm.switchMode(AppMode.Expert)
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
            assertFalse(h.sessionStore.isSetupDeferred())
            val relaunched = relaunch(h)
            runCurrent()
            assertEquals(AppRoute.SignupSetup, relaunched.route.value)
        }

    @Test
    fun logoutThenLoginResumesSetupAgain() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val vm = launchAndDefer(h)

            vm.logout()
            runCurrent()
            assertEquals(AppRoute.Login, vm.route.value)
            assertFalse(h.sessionStore.isSetupDeferred())

            h.api.loginHandler = {
                LoginData(
                    token = "access-2",
                    user = AuthUser(id = "u1", firstName = "Ada", lastName = "L", roles = listOf("client", "expert")),
                    refreshToken = "refresh-2",
                    expiresAt = 1_900_000_100L,
                    expiresIn = 3600L,
                )
            }
            val user = h.manager.login("a@b.co", "pw")
            vm.onLoggedIn(h.manager.resolvePostAuthDestination(user.userRoles))
            runCurrent()

            assertEquals(AppRoute.SignupSetup, vm.route.value)
        }

    @Test
    fun notNowAfterTheSessionEndedStaysOnLogin() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.convertedClientWithUnfinishedSetup()
            val vm = h.viewModel()
            runCurrent()
            h.events.emit(SessionEvent.Rejected)
            runCurrent()

            vm.onSetupDeferred()
            runCurrent()

            assertEquals(AppRoute.Login, vm.route.value)
            assertFalse(h.sessionStore.isSetupDeferred())
        }

    // ---- Phase 16.5: the 3 s Compose splash hold (D-07, D-08, D-09) ----

    @Test
    fun splashHoldsForThreeSecondsAfterComposition() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertTrue("held before composition", vm.splashHeld.value)

            vm.onSplashComposed()
            advanceTimeBy(AppViewModel.SPLASH_MIN_DISPLAY_MS - 1)
            runCurrent()
            assertTrue(vm.splashHeld.value)

            advanceTimeBy(1)
            runCurrent()
            assertFalse(vm.splashHeld.value)
        }

    @Test
    fun composedTwiceDoesNotRestartTheHold() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()

            vm.onSplashComposed()
            advanceTimeBy(2_000)
            runCurrent()
            vm.onSplashComposed()
            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(vm.splashHeld.value)
        }

    @Test
    fun shownRouteStaysSplashWhileHeldEvenWithAuthResolved() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            vm.onSplashComposed()
            advanceTimeBy(AppViewModel.SPLASH_MIN_DISPLAY_MS - 1)
            runCurrent()

            assertEquals(AppRoute.ClientHome, vm.route.value)
            assertTrue(vm.destinationResolved.value)
            assertEquals(AppRoute.Splash, shownRootRoute(vm.route.value, vm.splashHeld.value))

            advanceTimeBy(1)
            runCurrent()
            assertEquals(AppRoute.ClientHome, shownRootRoute(vm.route.value, vm.splashHeld.value))
        }

    @Test
    fun shownRouteStaysSplashWhenAuthOutlastsTheHold() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                me("client")
            }
            val vm = h.viewModel()
            runCurrent()
            vm.onSplashComposed()

            advanceTimeBy(AppViewModel.SPLASH_MIN_DISPLAY_MS)
            runCurrent()
            assertFalse(vm.splashHeld.value)
            assertEquals(AppRoute.Splash, shownRootRoute(vm.route.value, vm.splashHeld.value))

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS - AppViewModel.SPLASH_MIN_DISPLAY_MS + 1)
            runCurrent()
            assertEquals(AppRoute.ClientHome, shownRootRoute(vm.route.value, vm.splashHeld.value))
            gate.complete(Unit)
            runCurrent()
        }

    @Test
    fun loggedOutLaunchShowsLoginOnlyAfterTheHold() =
        runTest(main.testDispatcher) {
            val h = Harness(this, initial = null)
            val vm = h.viewModel()
            runCurrent()
            vm.onSplashComposed()

            assertEquals(AppRoute.Login, vm.route.value)
            assertEquals(AppRoute.Splash, shownRootRoute(vm.route.value, vm.splashHeld.value))

            advanceTimeBy(AppViewModel.SPLASH_MIN_DISPLAY_MS)
            runCurrent()
            assertEquals(AppRoute.Login, shownRootRoute(vm.route.value, vm.splashHeld.value))
        }

    @Test
    fun holdAndCapAreBothBoundedWhenAuthHangs() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                me("client")
            }
            val vm = h.viewModel()
            runCurrent()
            vm.onSplashComposed()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS + 1)
            runCurrent()

            assertFalse(vm.splashHeld.value)
            assertTrue(vm.destinationResolved.value)
            gate.complete(Unit)
            runCurrent()
        }

    @Test
    fun capPathLoadsPersistedOnboardingFlagBeforeRouteResolves() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.sessionStore.setHasSeenOnboarding(true)
            val gate = CompletableDeferred<Unit>()
            h.api.meHandler = {
                gate.await()
                me("client")
            }
            val vm = h.viewModel()

            advanceTimeBy(AppViewModel.SPLASH_HOLD_CAP_MS + 1)
            runCurrent()

            assertTrue(vm.destinationResolved.value)
            assertTrue(h.manager.hasSeenOnboarding.value)
            gate.complete(Unit)
            runCurrent()
        }
}
