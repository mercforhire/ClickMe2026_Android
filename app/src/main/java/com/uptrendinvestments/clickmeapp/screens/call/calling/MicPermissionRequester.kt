package com.uptrendinvestments.clickmeapp.screens.call.calling

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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.uptrendinvestments.clickmeapp.components.web.findActivity
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.PendingMicPermission

/** What to do before showing the system microphone prompt (RESEARCH Pattern 4). */
enum class MicPreRequestStep {
    AlreadyGranted,
    ShowRationale,
    LaunchRequest,
}

/** First half of the decision table: granted, explain first after one denial, or ask straight away. */
fun micPreRequestStep(
    granted: Boolean,
    shouldShowRationale: Boolean,
): MicPreRequestStep =
    when {
        granted -> MicPreRequestStep.AlreadyGranted
        shouldShowRationale -> MicPreRequestStep.ShowRationale
        else -> MicPreRequestStep.LaunchRequest
    }

/**
 * Second half, evaluated after the system prompt answered. Android reports "should show rationale" as false
 * both before the first ask and after a permanent denial, so this is only valid once a prompt has been shown
 * (RESEARCH Pitfall 12).
 */
fun micResultAfterRequest(
    granted: Boolean,
    shouldShowRationaleNow: Boolean,
): MicPermissionResult =
    when {
        granted -> MicPermissionResult.Granted
        shouldShowRationaleNow -> MicPermissionResult.DeniedOnce
        else -> MicPermissionResult.DeniedPermanently
    }

/** The microphone, plus notifications on API 33+ when not yet granted. Only the microphone decides the call. */
fun permissionsToRequest(
    sdkInt: Int,
    notificationsGranted: Boolean,
): Array<String> =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

/**
 * Fulfils the broker's [pending] microphone request: rationale dialog or system prompt as the decision table
 * says, then reports through [onResult]. Each pending id is handled once; the flags are saved so rotation
 * neither relaunches a request nor drops the rationale dialog. A notification denial never changes the result.
 */
@Composable
fun MicPermissionRequester(
    pending: PendingMicPermission?,
    onResult: (id: Long, result: MicPermissionResult) -> Unit,
    isGranted: (String) -> Boolean = defaultIsGranted(),
    shouldShowRationale: (String) -> Boolean = defaultShouldShowRationale(),
) {
    var handledId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showRationaleFor by rememberSaveable { mutableStateOf<Long?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val id = handledId ?: return@rememberLauncherForActivityResult
            val granted = results[Manifest.permission.RECORD_AUDIO] ?: isGranted(Manifest.permission.RECORD_AUDIO)
            onResult(id, micResultAfterRequest(granted, shouldShowRationale(Manifest.permission.RECORD_AUDIO)))
        }
    // Result ignored: a notification denial must never block the call.
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun launchRequest() = launcher.launch(permissionsToRequest(Build.VERSION.SDK_INT, isGranted(NOTIFICATIONS)))

    LaunchedEffect(pending?.id) {
        val id = pending?.id ?: return@LaunchedEffect
        if (handledId == id) return@LaunchedEffect
        handledId = id
        when (micPreRequestStep(isGranted(Manifest.permission.RECORD_AUDIO), shouldShowRationale(MIC))) {
            MicPreRequestStep.AlreadyGranted -> {
                onResult(id, MicPermissionResult.Granted)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isGranted(NOTIFICATIONS)) {
                    notificationLauncher.launch(NOTIFICATIONS)
                }
            }
            MicPreRequestStep.ShowRationale -> showRationaleFor = id
            MicPreRequestStep.LaunchRequest -> launchRequest()
        }
    }

    val rationaleId = showRationaleFor
    if (pending != null && rationaleId == pending.id) {
        MicRationaleDialog(
            onContinue = {
                showRationaleFor = null
                launchRequest()
            },
            onNotNow = {
                showRationaleFor = null
                onResult(rationaleId, MicPermissionResult.Cancelled)
            },
        )
    }
}

private const val MIC = Manifest.permission.RECORD_AUDIO
private const val NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

@Composable
private fun defaultIsGranted(): (String) -> Boolean {
    val context = LocalContext.current
    return { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun defaultShouldShowRationale(): (String) -> Boolean {
    val context = LocalContext.current
    return { permission ->
        context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
    }
}
