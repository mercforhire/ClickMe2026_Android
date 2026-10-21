@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp

import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
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

        fun viewModel() = AppViewModel(manager)
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
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(mapOf("id" to "t1"))) }
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

    // ---- debug scaffold toggle and route order ----

    @Test
    fun debugSwitchScaffoldTogglesBetweenClientAndExpertHome() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.clientLaunch()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(AppRoute.ClientHome, vm.route.value)

            vm.debugSwitchScaffold()
            assertEquals(AppRoute.ExpertHome, vm.route.value)

            vm.debugSwitchScaffold()
            assertEquals(AppRoute.ClientHome, vm.route.value)
        }

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
            val second = AppViewModel(relaunched)
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
