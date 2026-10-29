package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ContentMaxWidthTest {
    @get:Rule
    val rule = createComposeRule()

    private fun assertBounds(
        expectedWidth: Dp,
        expectedLeft: Dp,
    ) {
        val bounds = rule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
        assertEquals(expectedWidth.value, (bounds.right - bounds.left).value, TOLERANCE)
        assertEquals(expectedLeft.value, bounds.left.value, TOLERANCE)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capsAndCentersInBox() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) { Column(Modifier.contentMaxWidth().height(20.dp).testTag(TAG)) {} }
        }
        assertBounds(600.dp, 150.dp)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capsInColumnAndLazyItem() {
        var lazy by mutableStateOf(false)
        rule.setContent {
            if (lazy) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { Column(Modifier.contentMaxWidth().height(20.dp).testTag(TAG)) {} }
                }
            } else {
                Column(Modifier.fillMaxSize()) { Column(Modifier.contentMaxWidth().height(20.dp).testTag(TAG)) {} }
            }
        }
        assertBounds(600.dp, 150.dp)

        lazy = true
        rule.waitForIdle()
        assertBounds(600.dp, 150.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun fullWidthOnPhone() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) { Column(Modifier.contentMaxWidth().height(20.dp).testTag(TAG)) {} }
        }
        assertBounds(360.dp, 0.dp)
    }

    private companion object {
        const val TAG = "capped"
        const val TOLERANCE = 0.5f
    }
}
