package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Moshi
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest

/**
 * App-wide [Moshi] instance.
 *
 * Reflection-free: no `KotlinJsonAdapterFactory`. Codegen adapters are discovered by their
 * generated class name, so every DTO must carry `@JsonClass(generateAdapter = true)` and
 * an explicit `@Json(name = "...")` on every multi-word field (R8-safe).
 *
 * The one exception is [UpdateExpertTopicRequest], which has no codegen adapter: its hand-written
 * [UpdateExpertTopicRequestAdapter] is registered explicitly (no reflection, R8-safe) because the PATCH
 * body must carry explicit nulls that the default serializer omits.
 */
object MoshiProvider {
    val moshi: Moshi =
        Moshi
            .Builder()
            .add(InstantAdapter())
            .add(UpdateExpertTopicRequest::class.java, UpdateExpertTopicRequestAdapter())
            .build()
}
