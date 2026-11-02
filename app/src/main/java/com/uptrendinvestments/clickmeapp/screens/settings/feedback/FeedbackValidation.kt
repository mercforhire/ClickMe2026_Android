package com.uptrendinvestments.clickmeapp.screens.settings.feedback

/**
 * Minimum trimmed length of the feedback details (D-05). The backend schema is `details: z.string().min(10)`, so a
 * shorter text can only be rejected; the client checks it first and never sends it.
 */
const val FEEDBACK_DETAILS_MIN = 10

/** True when [details], after trimming, is shorter than [FEEDBACK_DETAILS_MIN]; whitespace counts as nothing. */
fun feedbackDetailsTooShort(details: String): Boolean = details.trim().length < FEEDBACK_DETAILS_MIN
