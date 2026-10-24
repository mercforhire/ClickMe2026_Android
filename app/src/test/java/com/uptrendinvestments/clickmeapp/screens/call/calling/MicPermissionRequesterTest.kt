package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.Manifest
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.PendingMicPermission
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val MIC = Manifest.permission.RECORD_AUDIO
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

    /** Launches whose input asks for the microphone (the multi-permission request). */
    fun micLaunches(): List<Pair<Int, Array<*>>> =
        launches.mapNotNull { (code, input) ->
            val permissions = input as? Array<*>
            if (permissions != null && permissions.contains(MIC)) code to permissions else null
        }

    fun answerMic(results: Map<String, Boolean>) {
        dispatchResult(micLaunches().last().first, results)
    }
}

@RunWith(RobolectricTestRunner::class)
class MicPermissionRequesterTest {
    @get:Rule
    val rule = createComposeRule()

    private val registry = RecordingRegistry()
    private val owner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
    private val results = mutableListOf<Pair<Long, MicPermissionResult>>()

    private var micGranted = false
    private var rationaleNow = false
    private var pending by mutableStateOf<PendingMicPermission?>(PendingMicPermission(1))
    private var recompositions by mutableIntStateOf(0)

    private fun start() {
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                ClickMeTheme {
                    // Reading the counter forces a recomposition when a test bumps it.
                    recompositions.let {
                        MicPermissionRequester(
                            pending = pending,
                            onResult = { id, result -> results += id to result },
                            isGranted = { permission -> if (permission == MIC) micGranted else true },
                            shouldShowRationale = { rationaleNow },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun alreadyGranted_reportsGrantedWithoutLaunching() {
        micGranted = true
        start()
        assertEquals(listOf(1L to MicPermissionResult.Granted), results)
        assertTrue(registry.micLaunches().isEmpty())
    }

    @Test
    fun shouldShowRationale_showsDialogAndContinueLaunchesRequest() {
        rationaleNow = true
        start()
        rule.onNodeWithText("Microphone access needed").assertIsDisplayed()
        assertTrue(registry.micLaunches().isEmpty())
        assertTrue(results.isEmpty())

        rule.onNodeWithTag(CALL_DIALOG_RATIONALE_CONTINUE_TAG).performClick()
        rule.waitForIdle()
        val launches = registry.micLaunches()
        assertEquals(1, launches.size)
        assertTrue(launches[0].second.contains(MIC))
        rule.onNodeWithText("Microphone access needed").assertDoesNotExist()
    }

    @Test
    fun rationaleNotNow_cancelsWithoutLaunching() {
        rationaleNow = true
        start()
        rule.onNodeWithTag(CALL_DIALOG_RATIONALE_NOT_NOW_TAG).performClick()
        rule.waitForIdle()
        assertEquals(listOf(1L to MicPermissionResult.Cancelled), results)
        assertTrue(registry.micLaunches().isEmpty())
    }

    @Test
    fun firstRequest_launchesDirectlyWithNoDialog() {
        start()
        assertEquals(1, registry.micLaunches().size)
        rule.onNodeWithText("Microphone access needed").assertDoesNotExist()
        assertTrue(results.isEmpty())
    }

    @Test
    fun notificationDenial_neverBlocksGrantedMic() {
        start()
        registry.answerMic(mapOf(MIC to true, NOTIFICATIONS to false))
        rule.waitForIdle()
        assertEquals(listOf(1L to MicPermissionResult.Granted), results)
    }

    @Test
    fun denied_withRationaleNow_isDeniedOnce() {
        start()
        rationaleNow = true
        registry.answerMic(mapOf(MIC to false))
        rule.waitForIdle()
        assertEquals(listOf(1L to MicPermissionResult.DeniedOnce), results)
    }

    @Test
    fun denied_withoutRationaleNow_isDeniedPermanently() {
        start()
        rationaleNow = false
        registry.answerMic(mapOf(MIC to false))
        rule.waitForIdle()
        assertEquals(listOf(1L to MicPermissionResult.DeniedPermanently), results)
    }

    @Test
    fun nullPending_showsAndLaunchesNothing() {
        pending = null
        start()
        assertTrue(registry.launches.isEmpty())
        assertTrue(results.isEmpty())
        rule.onNodeWithText("Microphone access needed").assertDoesNotExist()
    }

    @Test
    fun recomposition_doesNotRelaunchTheSamePendingId() {
        start()
        recompositions++
        rule.waitForIdle()
        pending = PendingMicPermission(1)
        rule.waitForIdle()
        assertEquals(1, registry.micLaunches().size)
    }

    @Test
    fun newPendingId_isHandledAgain() {
        start()
        pending = PendingMicPermission(2)
        rule.waitForIdle()
        assertEquals(2, registry.micLaunches().size)
    }
}
