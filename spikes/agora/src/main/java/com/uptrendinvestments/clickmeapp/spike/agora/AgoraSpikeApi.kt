package com.uptrendinvestments.clickmeapp.spike.agora

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Bare Retrofit surface for the Agora spike (D-11). Untyped maps keep the spike free of codegen. Non-2xx responses
 * surface as retrofit2.HttpException.
 */
interface AgoraSpikeApi {
    @POST("auth/login")
    suspend fun login(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Map<String, Any?>

    @POST("bookings/{id}/join")
    suspend fun joinCall(
        @Path("id") id: String,
        @Header("Authorization") bearer: String,
    ): Map<String, Any?>

    companion object {
        /** No logging interceptor on purpose: tokens and RTC credentials must not reach logcat. */
        fun create(): AgoraSpikeApi =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL + "/")
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
                .build()
                .create(AgoraSpikeApi::class.java)
    }
}
