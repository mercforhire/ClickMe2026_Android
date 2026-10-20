package com.uptrendinvestments.clickmeapp.config

import com.uptrendinvestments.clickmeapp.BuildConfig

/**
 * Typed access to the per-flavor build config (port of iOS `AppEnvironment`).
 *
 * Only screen-level and data-layer code may read this object. Shared components and `@Preview`
 * functions must take plain parameters instead.
 */
object AppConfig {
    val baseUrl: String get() = BuildConfig.BASE_URL
    val stripePublishableKey: String get() = BuildConfig.STRIPE_PUBLISHABLE_KEY
    val agoraAppId: String get() = BuildConfig.AGORA_APP_ID
    val flavor: String get() = BuildConfig.FLAVOR
}
