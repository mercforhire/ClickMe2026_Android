package com.uptrendinvestments.clickmeapp.spike.stripe

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Bare Retrofit surface for the Stripe spike (D-11). Bodies and responses are untyped maps so the spike needs no
 * codegen. Non-2xx responses surface as retrofit2.HttpException, whose errorBody carries the backend error envelope.
 */
interface StripeSpikeApi {
    @POST("auth/login")
    suspend fun login(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Map<String, Any?>

    @POST("me/payment-methods/setup-intent")
    suspend fun setupIntent(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Map<String, Any?>

    @GET("me/payment-methods")
    suspend fun paymentMethods(
        @Header("Authorization") bearer: String,
    ): Map<String, Any?>

    @POST("bookings/payment-intent")
    suspend fun paymentIntent(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Map<String, Any?>

    @POST("bookings/confirm")
    suspend fun confirm(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Map<String, Any?>

    companion object {
        /** No logging interceptor on purpose: tokens and client secrets must not reach logcat. */
        fun create(): StripeSpikeApi =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL + "/")
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
                .build()
                .create(StripeSpikeApi::class.java)
    }
}
