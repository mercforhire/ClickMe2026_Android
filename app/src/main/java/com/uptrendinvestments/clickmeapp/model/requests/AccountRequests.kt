package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * Phase 14 account-security request bodies. Each overrides toString() so a password, deletion token or free-text
 * feedback never reaches an error dialog, a test log or a crash report (threat T-14-01).
 */

/**
 * `POST /user/account/delete-request` body: step one of the two-step deletion. The server re-checks the password and
 * answers a single-use deletion token (CONTEXT D-05). Ports iOS `ClickMeAPI+Account.requestAccountDeletion`.
 */
@JsonClass(generateAdapter = true)
data class AccountDeletionRequest(
    val password: String,
) {
    override fun toString(): String = "AccountDeletionRequest(password=<redacted>)"
}

/**
 * `DELETE /user/account` body: step two of the deletion (CONTEXT D-05). A null [feedbackDetails] is omitted from the
 * JSON, never sent as null (the converter has no `serializeNulls`). [confirmPermanentAction] is always true.
 * Ports iOS `ClickMeAPI+Account.deleteAccount`. [toString] prints only the reason category.
 */
@JsonClass(generateAdapter = true)
data class DeleteAccountRequest(
    @Json(name = "deletion_token") val deletionToken: String,
    @Json(name = "reason_category") val reasonCategory: String,
    @Json(name = "feedback_details") val feedbackDetails: String? = null,
    @Json(name = "confirm_permanent_action") val confirmPermanentAction: Boolean = true,
) {
    override fun toString(): String = "DeleteAccountRequest(reasonCategory=$reasonCategory)"
}

/**
 * `PATCH /user/password` body. Ports iOS `UpdatePasswordRequest`. A wrong current password is a 403 and a weak new
 * one a 422 with `errors[]` naming `new_password`.
 */
@JsonClass(generateAdapter = true)
data class UpdatePasswordRequest(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "new_password") val newPassword: String,
) {
    override fun toString(): String = "UpdatePasswordRequest(passwords=<redacted>)"
}
