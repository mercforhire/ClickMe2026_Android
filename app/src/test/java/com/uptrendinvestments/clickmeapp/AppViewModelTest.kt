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
import kotlinx.coroutines.test.TestScope
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
}
