package com.uptrendinvestments.clickmeapp.screens.notifications

/** A run of notification body text; [bold] spans were wrapped in a matched pair of `**` markers. */
data class BodySpan(
    val text: String,
    val bold: Boolean,
)

/**
 * Splits [raw] into spans, turning each matched pair of `**` markers into a bold span.
 *
 * Pairs match left to right. An unmatched marker and a pair with nothing between them (`****`) stay literal text, and
 * adjacent literal pieces merge into one span. No backend string uses markers today (RESEARCH F-8), so this is
 * forward-compatible. Bold is weight only, never accent (UI-SPEC A8).
 */
fun parseNotificationBody(raw: String): List<BodySpan> {
    val spans = mutableListOf<BodySpan>()
    val literal = StringBuilder()
    var index = 0
    while (index < raw.length) {
        val open = raw.indexOf(MARKER, index)
        val close = if (open == -1) -1 else raw.indexOf(MARKER, open + MARKER.length)
        if (close == -1) {
            literal.append(raw, index, raw.length)
            break
        }
        val content = raw.substring(open + MARKER.length, close)
        if (content.isEmpty()) {
            literal.append(raw, index, close + MARKER.length)
        } else {
            literal.append(raw, index, open)
            if (literal.isNotEmpty()) {
                spans += BodySpan(literal.toString(), bold = false)
                literal.clear()
            }
            spans += BodySpan(content, bold = true)
        }
        index = close + MARKER.length
    }
    if (literal.isNotEmpty()) spans += BodySpan(literal.toString(), bold = false)
    return spans
}

/**
 * [raw] without the markers that [parseNotificationBody] treats as a matched pair. For accessibility text and the
 * system notification.
 */
fun stripBoldMarkers(raw: String): String = parseNotificationBody(raw).joinToString("") { it.text }

private const val MARKER = "**"
