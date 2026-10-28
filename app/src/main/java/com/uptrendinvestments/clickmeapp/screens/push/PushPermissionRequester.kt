package com.uptrendinvestments.clickmeapp.screens.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.PushPermissionGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** What the requester does for the current inputs. */
enum class NotificationPromptStep {
    /** The prompt is due but this is not the moment (login form, splash, live call). */
    Wait,

    /** Nothing to do: not due, or no runtime permission on this API level. */
    Skip,

    /** Already granted: record it as answered without showing anything. */
    RecordGranted,

    /** Show the system prompt now. */
    Launch,
}

private val LANDING_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome, AppRoute.SignupSetup)

/**
 * The pure decision table for the one-time notification prompt (D-01, D-02). The persisted flag behind
 * [needsPrompt] decides "asked", never the system rationale query: that query is false both before the first ask and
 * after a permanent denial (RESEARCH Pitfall 6).
 */
fun notificationPromptStep(
    sdkInt: Int,
    granted: Boolean,
    needsPrompt: Boolean,
    route: AppRoute,
    callActive: Boolean,
): NotificationPromptStep =
    when {
        !needsPrompt -> NotificationPromptStep.Skip
        sdkInt < Build.VERSION_CODES.TIRAMISU -> NotificationPromptStep.Skip
        granted -> NotificationPromptStep.RecordGranted
        route !in LANDING_ROUTES -> NotificationPromptStep.Wait
        callActive -> NotificationPromptStep.Wait
        else -> NotificationPromptStep.Launch
    }

/** Bridges [PushPermissionGate] and the call state to the composable. */
@HiltViewModel
class PushPermissionViewModel
    @Inject
    constructor(
        private val gate: PushPermissionGate,
        callGuard: CallGuard,
    ) : ViewModel() {
        val needsPrompt: StateFlow<Boolean> = gate.needsPrompt
        val callActive: StateFlow<Boolean> = callGuard.isCallActive

        fun onPromptLaunched() = gate.onPromptLaunched()

        fun onAnswered() = gate.onAnswered()

        fun onAlreadyGranted() = gate.onAlreadyGranted()

        fun onNotApplicable() = gate.onNotApplicable()
    }

/**
 * Non-visual root host of the one-time `POST_NOTIFICATIONS` prompt: right after a login or signup, once the landing
 * screen shows, never over the login form or a call, never twice (D-01, D-02). There is deliberately no rationale,
 * pre-prompt or nag UI anywhere. A denial changes nothing else (the FCM token is registered regardless).
 */
@Composable
fun PushPermissionRequester(
    route: AppRoute,
    viewModel: PushPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val needsPrompt by viewModel.needsPrompt.collectAsStateWithLifecycle()
    val callActive by viewModel.callActive.collectAsStateWithLifecycle()
    // The route can change while the system dialog is up (signup setup to home); never launch a second one.
    var launched by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.onAnswered() }

    LaunchedEffect(needsPrompt, route, callActive) {
        if (!needsPrompt) {
            launched = false
            return@LaunchedEffect
        }
        if (launched) return@LaunchedEffect
        val sdkInt = Build.VERSION.SDK_INT
        val granted =
            sdkInt >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        when (notificationPromptStep(sdkInt, granted, needsPrompt, route, callActive)) {
            // Only reachable with a due prompt: below API 33 there is nothing to ask.
            NotificationPromptStep.Skip -> viewModel.onNotApplicable()
            NotificationPromptStep.RecordGranted -> viewModel.onAlreadyGranted()
            NotificationPromptStep.Wait -> Unit
            NotificationPromptStep.Launch -> {
                launched = true
                viewModel.onPromptLaunched()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
