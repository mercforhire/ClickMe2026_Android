package com.uptrendinvestments.clickmeapp.screens.splash

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether this process already served the Compose splash. `AppViewModel` is per-activity, so a task swipe followed
 * by a notification tap creates a new one; this singleton is what makes "once per cold process start" true (A10).
 */
@Singleton
class SplashPlayedFlag
    @Inject
    constructor() {
        @Volatile
        var played: Boolean = false
    }
