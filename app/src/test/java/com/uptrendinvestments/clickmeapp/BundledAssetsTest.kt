package com.uptrendinvestments.clickmeapp

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledAssetsTest {
    private val drawables =
        mapOf(
            "mode_client" to R.drawable.mode_client,
            "mode_expert" to R.drawable.mode_expert,
            "onboarding_welcome" to R.drawable.onboarding_welcome,
            "onboarding_discover" to R.drawable.onboarding_discover,
            "onboarding_booking" to R.drawable.onboarding_booking,
            "onboarding_communication" to R.drawable.onboarding_communication,
        )

    @Test
    fun bundledIllustrations_decodeToNonEmptyBitmaps() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        drawables.forEach { (name, id) ->
            val bitmap = BitmapFactory.decodeResource(context.resources, id)
            assertNotNull("$name did not decode", bitmap)
            assertTrue("$name has no width", bitmap.width > 0)
        }
    }
}
