package com.uptrendinvestments.clickmeapp.screens.push

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.managers.PushPermissionGate
import com.uptrendinvestments.clickmeapp.managers.PushPreferences
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

/** Records every launch and lets a test dispatch the system's answer. */
private class RecordingRegistry : ActivityResultRegistry() {
    val launches = mutableListOf<Pair<Int, Any?>>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        launches += requestCode to input
    }

    fun notificationLaunches(): List<Int> = launches.filter { it.second == NOTIFICATIONS }.map { it.first }
}

/** The pure decision table, then the composable against a recording registry. */
@RunWith(RobolectricTestRunner::class)
class PushPermissionRequesterTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val registry = RecordingRegistry()
    private val owner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val starts = MutableStateFlow(0)
    private val callGuard = FakeCallGuard()
    private var route by mutableStateOf(AppRoute.ClientHome)

    private lateinit var prefs: PushPreferences
    private lateinit var gate: PushPermissionGate

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun start() {
        prefs = PushPreferences(testPreferencesDataStore(scope, tmp))
        gate = PushPermissionGate(starts, prefs, scope)
        val viewModel = PushPermissionViewModel(gate, callGuard)
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                PushPermissionRequester(route = route, viewModel = viewModel)
            }
        }
        rule.waitForIdle()
    }

    private fun login() {
        starts.value += 1
        rule.waitUntil(WAIT_MS) { gate.needsPrompt.value || registry.notificationLaunches().isNotEmpty() }
        rule.waitForIdle()
    }

    private fun asked(): Boolean = runBlocking { prefs.notificationPromptAsked() }

    @Test
    @Config(sdk = [33])
    fun launchesOnHomeAfterLogin() {
        start()
        login()

        assertEquals(1, registry.notificationLaunches().size)
        rule.waitUntil(WAIT_MS) { asked() }

        registry.dispatchResult(registry.notificationLaunches().single(), false)
        rule.waitForIdle()
        assertFalse(gate.needsPrompt.value)
    }

    @Test
    @Config(sdk = [33])
    fun waitsDuringCallThenLaunches() {
        callGuard.isCallActive.value = true
        start()
        starts.value += 1
        rule.waitUntil(WAIT_MS) { gate.needsPrompt.value }
        rule.waitForIdle()
        assertTrue(registry.notificationLaunches().isEmpty())

        callGuard.isCallActive.value = false
        rule.waitUntil(WAIT_MS) { registry.notificationLaunches().isNotEmpty() }
        rule.waitForIdle()

        assertEquals(1, registry.notificationLaunches().size)
    }

    @Test
    @Config(sdk = [33])
    fun waitsOnLoginThenLaunchesOnTheSetupHubOnce() {
        route = AppRoute.Login
        start()
        starts.value += 1
        rule.waitUntil(WAIT_MS) { gate.needsPrompt.value }
        rule.waitForIdle()
        assertTrue(registry.notificationLaunches().isEmpty())

        rule.runOnIdle { route = AppRoute.SignupSetup }
        rule.waitForIdle()
        rule.waitUntil(WAIT_MS) { registry.notificationLaunches().isNotEmpty() }
        // The route moving on while the system dialog is up must not launch a second one.
        rule.runOnIdle { route = AppRoute.ClientHome }
        rule.waitForIdle()

        assertEquals(1, registry.notificationLaunches().size)
    }

    @Test
    @Config(sdk = [33])
    fun alreadyGrantedRecordsWithoutLaunching() {
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(NOTIFICATIONS)
        start()
        starts.value += 1

        rule.waitUntil(WAIT_MS) { asked() }
        rule.waitForIdle()

        assertTrue(registry.notificationLaunches().isEmpty())
        assertFalse(gate.needsPrompt.value)
    }

    @Test
    @Config(sdk = [32])
    fun belowApi33NeverLaunches() {
        start()
        starts.value += 1
        rule.waitForIdle()
        rule.waitForIdle()

        assertTrue(registry.notificationLaunches().isEmpty())
        assertFalse(gate.needsPrompt.value)
        assertFalse(asked())
    }

    @Test
    fun decisionTable() {
        val api33 = Build.VERSION_CODES.TIRAMISU
        val homes = listOf(AppRoute.ClientHome, AppRoute.ExpertHome, AppRoute.SignupSetup)
        val notAskedYet = listOf(AppRoute.Splash, AppRoute.Login)
        val step = ::notificationPromptStep

        // Not due: Skip for every input.
        for (sdk in listOf(Build.VERSION_CODES.S_V2, api33)) {
            for (granted in listOf(true, false)) {
                for (route in AppRoute.entries) {
                    for (call in listOf(true, false)) {
                        assertEquals(NotificationPromptStep.Skip, step(sdk, granted, false, route, call))
                    }
                }
            }
        }
        // Below API 33.
        val below = Build.VERSION_CODES.S_V2
        assertEquals(NotificationPromptStep.Skip, step(below, false, true, AppRoute.ClientHome, false))
        assertEquals(NotificationPromptStep.RecordGranted, step(api33, true, true, AppRoute.Login, false))
        notAskedYet.forEach { assertEquals(NotificationPromptStep.Wait, step(api33, false, true, it, false)) }
        homes.forEach { assertEquals(NotificationPromptStep.Launch, step(api33, false, true, it, false)) }
        homes.forEach { assertEquals(NotificationPromptStep.Wait, step(api33, false, true, it, true)) }
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
