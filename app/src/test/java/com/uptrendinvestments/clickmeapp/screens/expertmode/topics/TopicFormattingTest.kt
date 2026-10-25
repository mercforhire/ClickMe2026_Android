package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicFormattingTest {
    @get:Rule
    val rule = createComposeRule()

    private fun render(vararg minutes: Int): List<String> {
        val out = mutableListOf<String>()
        rule.setContent { minutes.forEach { out += topicDurationText(it) } }
        rule.waitForIdle()
        return out
    }

    @Test
    fun durations_read_like_ios_format_duration() {
        assertEquals(
            listOf("30 min", "1 hour", "1 hr 30 min", "2 hours", "2 hr 30 min", "8 hours"),
            render(30, 60, 90, 120, 150, 480),
        )
    }
}
