package com.uptrendinvestments.clickmeapp.spike.agora

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.filterNotNull

/**
 * Throwaway spike (Phase 2, D-10). Delete this module after Phase 9 absorbs its findings.
 *
 * Logs in, fetches join credentials from the backend (D-13), asks for the microphone and starts the call inside a
 * microphone foreground service. The start happens only while this activity is resumed. The spike joins and holds
 * audio only: it never confirms the connection or ends the session (leaving is not ending).
 */
class MainActivity : ComponentActivity() {
    private val viewModel: AgoraSpikeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpikeScreen(viewModel, lifecycle)
                }
            }
        }
    }
}

@Composable
private fun SpikeScreen(
    viewModel: AgoraSpikeViewModel,
    lifecycle: Lifecycle,
) {
    val context = LocalContext.current
    val callState by CallService.state.collectAsState()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants[Manifest.permission.RECORD_AUDIO] == true || hasMicrophone(context)) {
                viewModel.join()
            } else {
                viewModel.record("Microphone permission is required")
            }
        }

    // Start the service only while resumed: a microphone foreground service cannot be started from the background.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { viewModel.pendingJoin }.filterNotNull().collect { config ->
                val intent =
                    Intent(context, CallService::class.java)
                        .setAction(CallService.ACTION_START)
                        .putExtra(CallService.EXTRA_TOKEN, config.rtcToken)
                        .putExtra(CallService.EXTRA_CHANNEL, config.channelName)
                        .putExtra(CallService.EXTRA_UID, config.uid)
                ContextCompat.startForegroundService(context, intent)
                viewModel.consumeJoin()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text("Agora spike: login", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.login() }) { Text("Log in") }
        Text(viewModel.status)

        Text("Join call", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(
            value = viewModel.bookingId,
            onValueChange = { viewModel.bookingId = it },
            label = { Text("Booking id") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = viewModel.loggedIn,
            onClick = { permissionLauncher.launch(requiredPermissions()) },
        ) {
            Text("Join call")
        }
        Button(
            onClick = {
                context.startService(Intent(context, CallService::class.java).setAction(CallService.ACTION_STOP))
            },
        ) {
            Text("Leave call")
        }
        Text("Call state: $callState")
        viewModel.logLines.forEach { Text(it) }
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

private fun hasMicrophone(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
