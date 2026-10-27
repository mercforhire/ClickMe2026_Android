package com.uptrendinvestments.clickmeapp.screens.chat

/**
 * Truncates [text] to at most [max] UTF-16 code units, never leaving half of a surrogate pair at the end.
 *
 * Kotlin `String.length` and the backend zod `max` both count UTF-16 code units, so this cap matches what the server
 * accepts. Used for report details here and for the message composer. The cap runs before any trimming.
 */
fun capText(
    text: String,
    max: Int,
): String {
    if (text.length <= max) return text
    val cut = text.substring(0, max)
    return if (cut.isNotEmpty() && Character.isHighSurrogate(cut.last())) cut.dropLast(1) else cut
}
