package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.FeedbackSubmissionData
import com.uptrendinvestments.clickmeapp.model.responses.FeedbackTypeItem
import com.uptrendinvestments.clickmeapp.model.responses.FeedbackTypesData

fun feedbackType(
    id: String = "bug_report",
    label: String = "Bug Report",
    description: String? = "Report a technical issue or error",
) = FeedbackTypeItem(id = id, label = label, description = description)

/** The five types `feedback.controller.js` serves, in the server's order. */
fun feedbackTypes() =
    FeedbackTypesData(
        types =
            listOf(
                feedbackType(),
                feedbackType("feature_request", "Feature Request", "Suggest a new feature or improvement"),
                feedbackType("general", "General Feedback", "Share general thoughts about the app"),
                feedbackType("billing", "Billing Issue", "Report a billing or payment concern"),
                feedbackType("content", "Content Feedback", "Feedback about expert content or sessions"),
            ),
    )

fun feedbackSubmission() = FeedbackSubmissionData(submissionId = "sub-1")
