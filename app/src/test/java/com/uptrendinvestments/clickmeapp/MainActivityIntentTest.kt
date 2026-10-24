package com.uptrendinvestments.clickmeapp

import android.content.Intent
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityIntentTest {
    @Test
    fun expandExtraTrue_returnsTrueOnceAndIsRemoved() {
        val intent = Intent().putExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, true)

        assertTrue(intent.consumeExpandCallExtra())
        assertFalse(intent.hasExtra(CallNotificationFactory.EXTRA_EXPAND_CALL))
        assertFalse(intent.consumeExpandCallExtra())
    }

    @Test
    fun noExtra_returnsFalse() {
        assertFalse(Intent(Intent.ACTION_MAIN).consumeExpandCallExtra())
    }

    @Test
    fun expandExtraFalse_returnsFalse() {
        val intent = Intent().putExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, false)

        assertFalse(intent.consumeExpandCallExtra())
    }

    @Test
    fun nullIntent_returnsFalse() {
        val intent: Intent? = null

        assertFalse(intent.consumeExpandCallExtra())
    }
}
