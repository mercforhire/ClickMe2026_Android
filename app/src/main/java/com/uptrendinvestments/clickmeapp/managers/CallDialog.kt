package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.network.ApiException

/**
 * Actions blocked during a live call (D-13: the call sites that exist today). Phases 10, 11 and 14 add
 * entries (edit availability, reschedule, change password, delete account, set up payouts). Each entry maps
 * to a `call_action_*` string in the UI.
 */
enum class RestrictedAction {
    SwitchModes,
    LogOut,
    AddPaymentMethod,
    CancelBooking,
}

/** Why joining a call failed, shown in the join-error dialog. */
sealed interface JoinErrorReason {
    /** A server-provided message, rendered as plain text (T-09-12). */
    data class Server(
        val message: String,
    ) : JoinErrorReason

    data object NotSetUpForInAppVoice : JoinErrorReason

    data object MissingMeetingLink : JoinErrorReason

    data object Network : JoinErrorReason

    data object Unknown : JoinErrorReason
}

/** Which copy the guard dialog shows. */
sealed interface GuardMessage {
    /** "End the call to <action>." */
    data class EndCallTo(
        val action: RestrictedAction,
    ) : GuardMessage

    /** "This session is in progress. End the call to <action>." */
    data class SessionInProgress(
        val action: RestrictedAction,
    ) : GuardMessage

    /** "You're already on a call. End it before starting another." */
    data object AlreadyOnCall : GuardMessage
}

/** Dialogs queued by `CallCenter` and rendered by the root overlay. */
sealed interface CallDialog {
    data class JoinError(
        val reason: JoinErrorReason,
    ) : CallDialog

    data object MicPermissionDenied : CallDialog

    data class Guard(
        val message: GuardMessage,
    ) : CallDialog

    data object TimeLimit : CallDialog
}

/** Maps a join failure to its dialog reason: server text when present, else connectivity or unknown. */
fun ApiException.toJoinErrorReason(): JoinErrorReason =
    when (this) {
        is ApiException.Http -> userMessage?.let { JoinErrorReason.Server(it) } ?: JoinErrorReason.Unknown
        is ApiException.Network -> JoinErrorReason.Network
        is ApiException.Decoding -> JoinErrorReason.Unknown
    }
