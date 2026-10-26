package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes

/**
 * Longest accept message the backend `acceptSchema` allows (`message: z.string().max(1000).optional()`), measured in
 * Kotlin `String` length (UTF-16 units) as D-02 and D-08 specify.
 */
const val ACCEPT_MESSAGE_MAX_CHARS = 1000

/** The character counter under the message and note fields appears once the text reaches this length (D-08). */
internal const val NOTE_COUNTER_FROM = 800

/**
 * The accept `message` to send: the trimmed [raw] text capped at [ACCEPT_MESSAGE_MAX_CHARS], or null when it is blank
 * so the request body omits the key (`{}`), which the strict backend schema accepts.
 */
fun acceptMessage(raw: String): String? =
    raw
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let { capNotes(it, ACCEPT_MESSAGE_MAX_CHARS) }
