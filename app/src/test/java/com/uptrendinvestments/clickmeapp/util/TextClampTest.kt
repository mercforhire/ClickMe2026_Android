package com.uptrendinvestments.clickmeapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextClampTest {
    @Test
    fun shortTextIsUnchanged() {
        assertEquals("abc", "abc".clampLength(3))
        assertEquals("abc", "abc".clampLength(10))
    }

    @Test
    fun longTextIsCutAtTheLimit() {
        assertEquals("ab", "abcd".clampLength(2))
        assertEquals("", "abcd".clampLength(0))
    }

    @Test
    fun cutNeverLeavesALoneHighSurrogate() {
        val grin = "😀" // one emoji, two UTF-16 units
        assertEquals("a", "a${grin}b".clampLength(2))
        assertEquals("a$grin", "a${grin}b".clampLength(3))
    }

    @Test
    fun wholeEmojiThatFitsIsKept() {
        val grin = "😀"
        assertEquals(grin, "$grin$grin".clampLength(2))
    }
}
