package com.uptrendinvestments.clickmeapp.spike.stripe

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

/** A saved card row from GET /me/payment-methods. */
data class CardRow(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)

/** 3DS challenge from a 402 PAYMENT_REQUIRES_ACTION. Held in memory only and cleared after use. */
data class Sca(
    val clientSecret: String,
    val stripeAccount: String,
)

/** One-shot request for the Activity to present PaymentSheet in SetupIntent mode. */
data class SetupRequest(
    val clientSecret: String,
    val customerId: String,
    val ephemeralKey: String,
)

/**
 * State holder for the Stripe spike (D-12). State lives in Compose mutableStateOf so it survives the round trip
 * through Stripe's activities. Log lines carry fixed evidence prefixes and never include the token, client_secret,
 * ephemeral_key or the stripe_account value.
 */
class StripeSpikeViewModel : ViewModel() {
    private val api = StripeSpikeApi.create()
    private val moshi = Moshi.Builder().build()

    var token by mutableStateOf<String?>(null)
        private set
    var logLines by mutableStateOf<List<String>>(emptyList())
        private set
    var cards by mutableStateOf<List<CardRow>>(emptyList())
        private set
    var selectedPm by mutableStateOf<String?>(null)
        private set
    var challenge by mutableStateOf<Sca?>(null)
        private set
    var setupRequest by mutableStateOf<SetupRequest?>(null)
        private set

    var expertId by mutableStateOf(BuildConfig.EXPERT_ID)
    var topicId by mutableStateOf(BuildConfig.TOPIC_ID)
    var selectedDate by mutableStateOf(BuildConfig.SELECTED_DATE)
    var timeSlot by mutableStateOf(BuildConfig.TIME_SLOT)
    var startTime by mutableStateOf(BuildConfig.START_TIME)
    var durationMinutes by mutableStateOf(BuildConfig.DURATION_MINUTES)

    private var lastConfirmBody: Map<String, Any?>? = null

    private val bearer: String get() = "Bearer ${token.orEmpty()}"

    private fun log(line: String) {
        Log.i(TAG, line)
        logLines = listOf(line) + logLines
    }

    fun login(
        email: String,
        password: String,
    ) {
        viewModelScope.launch {
            try {
                val response = api.login(mapOf("email" to email.trim(), "password" to password))
                token = (response["data"] as? Map<*, *>)?.get("token") as? String
                log(if (token != null) "login: 200 token held in memory" else "login: 200 but no token in response")
            } catch (e: HttpException) {
                log("login: ${e.code()} ${errorCode(e.response()?.errorBody()?.string())}")
            } catch (e: IOException) {
                log("login: network error ${e.javaClass.simpleName}")
            }
        }
    }

    /** Asks the backend for a SetupIntent and hands it to the Activity, which presents PaymentSheet. */
    fun createSetupIntent() {
        viewModelScope.launch {
            try {
                val data = api.setupIntent(bearer, emptyMap())["data"] as? Map<*, *>
                val clientSecret = data?.get("client_secret") as? String
                val customerId = data?.get("customer_id") as? String
                val ephemeralKey = data?.get("ephemeral_key") as? String
                if (clientSecret == null || customerId == null || ephemeralKey == null) {
                    log("setup: Failed missing client_secret/customer_id/ephemeral_key in response")
                } else {
                    log("setup: SetupIntent created, presenting PaymentSheet")
                    setupRequest = SetupRequest(clientSecret, customerId, ephemeralKey)
                }
            } catch (e: HttpException) {
                log("setup: Failed ${e.code()} ${errorCode(e.response()?.errorBody()?.string())}")
            } catch (e: IOException) {
                log("setup: Failed network error ${e.javaClass.simpleName}")
            }
        }
    }

    fun consumeSetupRequest() {
        setupRequest = null
    }

    fun onSetupResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> log("setup: Completed")
            is PaymentSheetResult.Canceled -> log("setup: Canceled")
            is PaymentSheetResult.Failed -> log("setup: Failed ${result.error.message}")
        }
    }

    /** PaymentSheet does not return the pm_ id, so the saved card is picked from the list endpoint. */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            try {
                val data = api.paymentMethods(bearer)["data"] as? Map<*, *>
                val rows =
                    (data?.get("payment_methods") as? List<*>).orEmpty().mapNotNull { item ->
                        val m = item as? Map<*, *> ?: return@mapNotNull null
                        val id = m["id"] as? String ?: return@mapNotNull null
                        CardRow(
                            id = id,
                            brand = m["brand"] as? String ?: "card",
                            last4 = m["last4"] as? String ?: "????",
                            isDefault = m["is_default"] as? Boolean ?: false,
                        )
                    }
                cards = rows
                // 4000 0027 6000 3184 is the Stripe test card that always requires 3DS.
                val pick =
                    rows.firstOrNull { it.last4 == "3184" } ?: rows.firstOrNull { it.isDefault } ?: rows.firstOrNull()
                selectedPm = pick?.id
                log("methods: ${rows.size} cards, selected •••• ${pick?.last4 ?: "none"}")
            } catch (e: HttpException) {
                log("methods: ${e.code()} ${errorCode(e.response()?.errorBody()?.string())}")
            } catch (e: IOException) {
                log("methods: network error ${e.javaClass.simpleName}")
            }
        }
    }

    fun selectCard(id: String) {
        selectedPm = id
    }

    /** Creates a PaymentIntent with a fresh booking_attempt_id, then confirms it server-side (Path A). */
    fun book() {
        val pm = selectedPm
        if (pm == null) {
            log("payment-intent: skipped, load cards and select one first")
            return
        }
        viewModelScope.launch {
            try {
                val intentBody =
                    mapOf(
                        "expert_id" to expertId.trim(),
                        "topic_id" to topicId.trim(),
                        "selected_date" to selectedDate.trim(),
                        "time_slot" to timeSlot.trim(),
                        "duration_minutes" to (durationMinutes.trim().toIntOrNull() ?: 0),
                        // Fresh UUID per tap; this key belongs to payment-intent only. The confirm schema is strict.
                        "booking_attempt_id" to UUID.randomUUID().toString(),
                    )
                val intent = api.paymentIntent(bearer, intentBody)
                // The body is bare, but tolerate a data envelope.
                val payload = (intent["data"] as? Map<*, *>) ?: intent
                log("payment-intent: 200 amount=${payload["total_amount"]} ${payload["currency"]}")
                val paymentIntentId = payload["payment_intent_id"] as? String
                if (paymentIntentId == null) {
                    log("confirm: skipped, no payment_intent_id in response")
                    return@launch
                }
                // Exactly six keys. Never add booking_attempt_id here: the strict schema answers 422.
                val confirmBody: Map<String, Any?> =
                    mapOf(
                        "payment_intent_id" to paymentIntentId,
                        "expert_id" to expertId.trim(),
                        "topic_id" to topicId.trim(),
                        "start_time" to startTime.trim(),
                        "meeting_type" to "in_app_voice",
                        "stripe_payment_method_id" to pm,
                    )
                lastConfirmBody = confirmBody
                postConfirm(confirmBody, "confirm")
            } catch (e: HttpException) {
                log("payment-intent: ${e.code()} ${errorCode(e.response()?.errorBody()?.string())}")
            } catch (e: IOException) {
                log("payment-intent: network error ${e.javaClass.simpleName}")
            }
        }
    }

    /** Result of the PaymentLauncher 3DS step on the connected account. */
    fun onPaymentResult(result: PaymentResult) {
        // stripe_account is single-use and never persisted.
        challenge = null
        when (result) {
            is PaymentResult.Completed -> {
                log("3ds: Completed")
                val body = lastConfirmBody
                if (body == null) {
                    log("confirm-retry: skipped, no previous confirm body")
                } else {
                    viewModelScope.launch { postConfirm(body, "confirm-retry") }
                }
            }
            is PaymentResult.Canceled -> log("3ds: Canceled")
            is PaymentResult.Failed -> log("3ds: Failed ${result.throwable.message}")
        }
    }

    private suspend fun postConfirm(
        body: Map<String, Any?>,
        prefix: String,
    ) {
        try {
            val data = api.confirm(bearer, body)["data"] as? Map<*, *>
            log("$prefix: 201 ${data?.get("status")}")
        } catch (e: HttpException) {
            handleConfirmError(e, prefix)
        } catch (e: IOException) {
            log("$prefix: network error ${e.javaClass.simpleName}")
        }
    }

    private fun handleConfirmError(
        e: HttpException,
        prefix: String,
    ) {
        val envelope = parseMap(e.response()?.errorBody()?.string())
        val code = envelope?.get("code") as? String
        if (e.code() == HTTP_PAYMENT_REQUIRED && code == "PAYMENT_REQUIRES_ACTION") {
            val detail = envelope["detail"] as? Map<*, *>
            val clientSecret = detail?.get("client_secret") as? String
            val stripeAccount = detail?.get("stripe_account") as? String
            log("$prefix: 402 PAYMENT_REQUIRES_ACTION (stripe_account present: ${stripeAccount != null})")
            if (clientSecret != null && stripeAccount != null) {
                challenge = Sca(clientSecret, stripeAccount)
            }
        } else {
            val hint =
                if (code ==
                    "LONG_LEAD_REQUIRES_SAVED_CARD"
                ) {
                    " (slot is more than 6 days out, pick a nearer slot)"
                } else {
                    ""
                }
            log("$prefix: ${e.code()} ${code ?: "no code"}$hint")
        }
    }

    private fun parseMap(json: String?): Map<*, *>? =
        try {
            if (json.isNullOrBlank()) null else moshi.adapter(Map::class.java).fromJson(json)
        } catch (e: IOException) {
            null
        }

    private fun errorCode(json: String?): String = (parseMap(json)?.get("code") as? String) ?: "no code"

    private companion object {
        const val TAG = "StripeSpike"
        const val HTTP_PAYMENT_REQUIRED = 402
    }
}
