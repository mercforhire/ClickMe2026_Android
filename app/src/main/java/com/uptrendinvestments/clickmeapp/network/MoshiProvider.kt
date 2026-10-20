package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Moshi

/**
 * App-wide [Moshi] instance.
 *
 * Reflection-free: no `KotlinJsonAdapterFactory`. Codegen adapters are discovered by their
 * generated class name, so every DTO must carry `@JsonClass(generateAdapter = true)` and
 * an explicit `@Json(name = "...")` on every multi-word field (R8-safe).
 */
object MoshiProvider {
    val moshi: Moshi = Moshi.Builder().add(InstantAdapter()).build()
}
