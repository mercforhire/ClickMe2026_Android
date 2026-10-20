package com.uptrendinvestments.clickmeapp.spike.agora

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Throwaway spike (Phase 2, D-10). Delete this module after Phase 7 (Stripe) / Phase 9 (Agora) absorbs its findings.
 *
 * Plan 02-04 ships the login form only; plan 02-08 adds the Agora call flow. The token lives in memory only (D-11).
 */
class MainActivity : ComponentActivity() {
    private val api = AgoraSpikeApi.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginSection(api)
                }
            }
        }
    }
}

@Composable
private fun LoginSection(api: AgoraSpikeApi) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(BuildConfig.CLIENT_EMAIL) }
    var password by remember { mutableStateOf("") }
    var token by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("not logged in") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text("Agora spike: login", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    status =
                        try {
                            val response = api.login(mapOf("email" to email.trim(), "password" to password))
                            val data = response["data"] as? Map<*, *>
                            token = data?.get("token") as? String
                            val hasToken = token != null
                            if (hasToken) "login: 200 token held in memory" else "login: 200 but no token in response"
                        } catch (e: HttpException) {
                            "login: ${e.code()}"
                        } catch (e: java.io.IOException) {
                            "login: network error ${e.javaClass.simpleName}"
                        }
                    Log.i("AgoraSpike", status)
                }
            },
        ) {
            Text("Log in")
        }
        Text(status)
        Text(if (token != null) "Ready for the call flow (plan 02-08)" else "")
    }
}
