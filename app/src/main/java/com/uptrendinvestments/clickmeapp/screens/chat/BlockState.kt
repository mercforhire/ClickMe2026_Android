package com.uptrendinvestments.clickmeapp.screens.chat

/** The block flags a `GET /chats` scan found for one thread. */
data class ScannedBlock(
    val isBlocked: Boolean,
    val blockedByMe: Boolean,
)

/** What the thread composer area shows. */
enum class ComposerMode {
    Normal,
    BlockedByMe,
    BlockedByPeer,
    Unavailable,
}

/** Which entry the thread's overflow menu offers next to Report. */
enum class ThreadMenuEntry {
    BlockOrReport,
    UnblockOrReport,
    ReportOnly,
}

/**
 * Block state of one thread, following CHAT-UNBLOCK section 5 (UI matrix), section 6 (local state after each
 * outcome) and section 7 (legacy blocks), with decisions D-03 and D-08.
 *
 * - block 200: blocked by me. unblock 200: not blocked.
 * - unblock 403 NOT_BLOCKER: still blocked, not by me, and Unblock is latched off ([unblockDenied]) for the rest of
 *   the screen session, as on iOS.
 * - send 403 THREAD_BLOCKED: blocked.
 * - A legacy block ([isBlocked] with [blockedByMe] false) is treated as blocked by the peer: Unblock is hidden.
 * - A scan of `GET /chats` that does not list the thread must not mean "unblocked": empty threads are absent from
 *   `GET /chats` (A4 resolution), so the state is left alone unless this session blocked the thread.
 */
data class BlockState(
    val isBlocked: Boolean = false,
    val blockedByMe: Boolean = false,
    val unblockDenied: Boolean = false,
) {
    /**
     * Applies a `GET /chats` scan. [found] is null when the thread is not listed. The unblock latch survives while
     * the scan still says blocked and is cleared when the thread is no longer blocked, so a scan never re-enables
     * Unblock for a peer block.
     */
    fun applyScan(
        found: ScannedBlock?,
        sessionBlockedByMe: Boolean,
    ): BlockState =
        when {
            found != null ->
                BlockState(
                    isBlocked = found.isBlocked,
                    blockedByMe = found.blockedByMe,
                    unblockDenied = unblockDenied && found.isBlocked,
                )
            sessionBlockedByMe -> BlockState(isBlocked = true, blockedByMe = true)
            else -> this
        }

    fun onBlockSucceeded(): BlockState = BlockState(isBlocked = true, blockedByMe = true)

    fun onUnblockSucceeded(): BlockState = BlockState(isBlocked = false, blockedByMe = false)

    /** Unblock was rejected with 403 NOT_BLOCKER: blocked by the peer, and the Unblock button stays off. */
    fun onNotBlocker(): BlockState = BlockState(isBlocked = true, blockedByMe = false, unblockDenied = true)

    /** A send was rejected with 403 THREAD_BLOCKED. Who blocked is unknown, so [blockedByMe] is kept. */
    fun onThreadBlocked(): BlockState = copy(isBlocked = true)

    val showsBlock: Boolean get() = !isBlocked

    val showsUnblock: Boolean get() = isBlocked && blockedByMe && !unblockDenied

    val menuEntry: ThreadMenuEntry
        get() =
            when {
                !isBlocked -> ThreadMenuEntry.BlockOrReport
                showsUnblock -> ThreadMenuEntry.UnblockOrReport
                else -> ThreadMenuEntry.ReportOnly
            }

    /** The composer mode implied by the block flags alone: blocked by me, blocked by the peer, or normal. */
    val ownComposerMode: ComposerMode
        get() =
            when {
                !isBlocked -> ComposerMode.Normal
                blockedByMe -> ComposerMode.BlockedByMe
                else -> ComposerMode.BlockedByPeer
            }
}

/** Blocked modes win (the server checks THREAD_BLOCKED first), then an unavailable recipient, then Normal. */
fun composerMode(
    block: BlockState,
    recipientUnavailable: Boolean,
): ComposerMode =
    when {
        block.isBlocked -> block.ownComposerMode
        recipientUnavailable -> ComposerMode.Unavailable
        else -> ComposerMode.Normal
    }
