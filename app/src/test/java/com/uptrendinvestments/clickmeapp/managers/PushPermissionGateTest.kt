package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Real dispatchers: DataStore does its file IO off the test scheduler, so conditions are awaited, not stepped. */
class PushPermissionGateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun prefs() = PushPreferences(testPreferencesDataStore(scope, tmp, "push.preferences_pb"))

    private suspend fun eventually(condition: suspend () -> Boolean) {
        withTimeout(TIMEOUT_MS) { while (!condition()) delay(POLL_MS) }
    }

    /** Lets the gate's collector run against the current state before asserting that nothing happened. */
    private suspend fun settle() = delay(SETTLE_MS)

    @Test
    fun loginRaisesNeedsPromptOnce() =
        runBlocking {
            val starts = MutableStateFlow(0)
            val prefs = prefs()
            val gate = PushPermissionGate(starts, prefs, scope)
            settle()
            assertFalse(gate.needsPrompt.value)

            starts.value = 1
            eventually { gate.needsPrompt.value }

            gate.onPromptLaunched()
            eventually { prefs.notificationPromptAsked() }

            gate.onAnswered()
            assertFalse(gate.needsPrompt.value)

            // A second login does not raise it again.
            starts.value = 2
            settle()
            assertFalse(gate.needsPrompt.value)
        }

    @Test
    fun askedSurvivesLogoutAndANewGate() =
        runBlocking {
            val store = testPreferencesDataStore(scope, tmp, "push.preferences_pb")
            val session = SessionStore(testPreferencesDataStore(scope, tmp, "session.preferences_pb"))
            val first = PushPermissionGate(MutableStateFlow(1), PushPreferences(store), scope)
            eventually { first.needsPrompt.value }
            first.onPromptLaunched()
            eventually { PushPreferences(store).notificationPromptAsked() }
            first.onAnswered()

            // Logout clears the session store but not the push file; a new gate and a new login stay quiet.
            session.clear()
            val second = PushPermissionGate(MutableStateFlow(1), PushPreferences(store), scope)
            settle()

            assertFalse(second.needsPrompt.value)
        }

    @Test
    fun restoreDoesNotPrompt() =
        runBlocking {
            // sessionStarts stays 0: a session restore never increments it.
            val gate = PushPermissionGate(MutableStateFlow(0), prefs(), scope)

            settle()

            assertFalse(gate.needsPrompt.value)
        }

    @Test
    fun loginBeforeTheGateExistedStillCounts() =
        runBlocking {
            val gate = PushPermissionGate(MutableStateFlow(1), prefs(), scope)

            eventually { gate.needsPrompt.value }

            assertTrue(gate.needsPrompt.value)
        }

    @Test
    fun alreadyGrantedIsRecordedAsAnswered() =
        runBlocking {
            val prefs = prefs()
            val gate = PushPermissionGate(MutableStateFlow(1), prefs, scope)
            eventually { gate.needsPrompt.value }

            gate.onAlreadyGranted()
            eventually { prefs.notificationPromptAsked() }

            assertFalse(gate.needsPrompt.value)
        }

    @Test
    fun notApplicableClearsWithoutRecordingAsked() =
        runBlocking {
            val prefs = prefs()
            val gate = PushPermissionGate(MutableStateFlow(1), prefs, scope)
            eventually { gate.needsPrompt.value }

            gate.onNotApplicable()
            settle()

            assertFalse(gate.needsPrompt.value)
            assertFalse(prefs.notificationPromptAsked())
        }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 10L
        const val SETTLE_MS = 200L
    }
}
