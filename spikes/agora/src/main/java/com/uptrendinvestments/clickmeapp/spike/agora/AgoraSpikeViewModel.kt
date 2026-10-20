package com.uptrendinvestments.clickmeapp.spike.agora

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Credentials for the Agora channel, parsed from the backend join response. */
data class JoinConfig(
    val rtcToken: String,
    val uid: Int,
    val channelName: String,
)

/**
 * Spike state holder (Phase 2, D-10, D-13). The auth token lives in memory only (D-11) and nothing sensitive is logged.
 */
class AgoraSpikeViewModel : ViewModel() {
    private val api = AgoraSpikeApi.create()
    private val moshi = Moshi.Builder().build()
    private var authToken: String? = null

    var email by mutableStateOf(BuildConfig.CLIENT_EMAIL)
    var password by mutableStateOf("")
    var bookingId by mutableStateOf(BuildConfig.BOOKING_ID)
    var loggedIn by mutableStateOf(false)
        private set
    var status by mutableStateOf("not logged in")
        private set

    /** One-shot join credentials; the activity consumes it while resumed and starts the service. */
    var pendingJoin by mutableStateOf<JoinConfig?>(null)
        private set

    val logLines = mutableStateListOf<String>()

    fun login() {
        viewModelScope.launch {
            val line =
                try {
                    val response = api.login(mapOf("email" to email.trim(), "password" to password))
                    val data = response["data"] as? Map<*, *>
                    authToken = data?.get("token") as? String
                    loggedIn = authToken != null
                    if (loggedIn) "login: 200 token held in memory" else "login: 200 but no token in response"
                } catch (e: HttpException) {
                    "login: ${e.code()}"
                } catch (e: IOException) {
                    "login: network error ${e.javaClass.simpleName}"
                }
            record(line)
            status = line
        }
    }

    fun join() {
        val bearer = authToken
        if (bearer == null) {
            record("join-api: not logged in")
            return
        }
        viewModelScope.launch {
            try {
                val response = api.joinCall(bookingId.trim(), "Bearer $bearer")
                val data = response["data"] as? Map<*, *>
                val connectionType = data?.get("connection_type")
                record("join-api: 200 connection_type=$connectionType")
                val agora = data?.get("agora_config") as? Map<*, *>
                val rtc = agora?.get("rtc_token") as? String
                val uid = (agora?.get("uid") as? Number)?.toInt()
                val channel = agora?.get("channel_name") as? String
                if (agora == null || rtc == null || uid == null || channel == null) {
                    record("join-api: no agora_config")
                } else {
                    pendingJoin = JoinConfig(rtc, uid, channel)
                }
            } catch (e: HttpException) {
                record("join-api: ${e.code()} ${errorCode(e)}")
            } catch (e: IOException) {
                record("join-api: network error ${e.javaClass.simpleName}")
            }
        }
    }

    fun consumeJoin() {
        pendingJoin = null
    }

    fun record(line: String) {
        Log.i(TAG, line)
        logLines.add(line)
        if (logLines.size > MAX_LINES) logLines.removeAt(0)
    }

    /**
     * 503 CALL_UNAVAILABLE means Agora is not configured server-side. Other 4xx usually means the booking is not
     * confirmed or is outside its join window: retry when the iOS Join button is active.
     */
    private fun errorCode(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: return ""
        val decoded = runCatching { moshi.adapter(Any::class.java).fromJson(body) }.getOrNull()
        val parsed = decoded as? Map<*, *> ?: return ""
        val nested = parsed["error"] as? Map<*, *>
        return (parsed["code"] ?: nested?.get("code"))?.toString().orEmpty()
    }

    private companion object {
        const val TAG = "AgoraSpike"
        const val MAX_LINES = 8
    }
}
