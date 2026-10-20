package com.uptrendinvestments.clickmeapp.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigProdTest {
    @Test
    fun baseUrlPointsAtProductionBackend() {
        assertEquals("https://clickme-backend-production.up.railway.app/api/v1", AppConfig.baseUrl)
    }

    @Test
    fun stripeKeyIsLiveModePublishableKey() {
        assertTrue(AppConfig.stripePublishableKey.startsWith("pk_live_"))
    }

    @Test
    fun agoraAppIdMatchesIos() {
        assertEquals("483eaa4d11fb44b6acc91bf5ceae36c7", AppConfig.agoraAppId)
    }

    @Test
    fun flavorIsProd() {
        assertEquals("prod", AppConfig.flavor)
    }
}
