package com.uptrendinvestments.clickmeapp.util

/**
 * Cuts the string to at most [max] UTF-16 units without splitting a surrogate pair: when the cut would leave a lone
 * high surrogate (half an emoji) at the end, that half is dropped too, so the server never receives malformed text.
 */
fun String.clampLength(max: Int): String {
    if (length <= max) return this
    if (max <= 0) return ""
    val cut = if (Character.isHighSurrogate(this[max - 1])) max - 1 else max
    return substring(0, cut)
}
