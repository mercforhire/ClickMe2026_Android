package com.uptrendinvestments.clickmeapp.screens.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockStateTest {
    @Test
    fun blockSucceededMeansBlockedByMe() {
        assertEquals(BlockState(true, true, false), BlockState().onBlockSucceeded())
    }

    @Test
    fun unblockSucceededClearsBlock() {
        assertEquals(BlockState(false, false, false), BlockState(true, true).onUnblockSucceeded())
    }

    @Test
    fun notBlockerLatchesUnblockOff() {
        val state = BlockState(true, true).onNotBlocker()
        assertEquals(BlockState(true, false, true), state)
        assertFalse(state.showsUnblock)
    }

    @Test
    fun threadBlockedSetsBlockedKeepingBlockedByMe() {
        assertEquals(BlockState(true, false, false), BlockState().onThreadBlocked())
        assertEquals(BlockState(true, true, false), BlockState(false, true).onThreadBlocked())
    }

    @Test
    fun scanThatDoesNotFindTheThreadLeavesStateUnchanged() {
        val state = BlockState(false, false, false)
        assertEquals(state, state.applyScan(null, sessionBlockedByMe = false))
    }

    @Test
    fun scanThatDoesNotFindTheThreadAfterSessionBlockIsBlockedByMe() {
        assertEquals(BlockState(true, true), BlockState().applyScan(null, sessionBlockedByMe = true))
    }

    @Test
    fun scanFindingUnblockedThreadClearsTheLatch() {
        val latched = BlockState(true, false, true)
        val result = latched.applyScan(ScannedBlock(isBlocked = false, blockedByMe = false), sessionBlockedByMe = false)
        assertEquals(BlockState(false, false, false), result)
    }

    @Test
    fun scanNeverReEnablesUnblockWhileStillBlocked() {
        val latched = BlockState().onBlockSucceeded().onNotBlocker()
        val result = latched.applyScan(ScannedBlock(isBlocked = true, blockedByMe = true), sessionBlockedByMe = true)
        assertTrue(result.isBlocked)
        assertFalse(result.showsUnblock)
    }

    @Test
    fun scanTakesBothFlagsWhenNotLatched() {
        val result = BlockState().applyScan(ScannedBlock(isBlocked = true, blockedByMe = true), false)
        assertEquals(BlockState(true, true, false), result)
        assertTrue(result.showsUnblock)
        assertFalse(result.showsBlock)
    }

    @Test
    fun legacyBlockIsBlockedByPeerAndReportOnly() {
        val legacy = BlockState().applyScan(ScannedBlock(isBlocked = true, blockedByMe = false), false)
        assertEquals(ComposerMode.BlockedByPeer, composerMode(legacy, recipientUnavailable = false))
        assertEquals(ThreadMenuEntry.ReportOnly, legacy.menuEntry)
        assertFalse(legacy.showsUnblock)
    }

    @Test
    fun notBlockedShowsBlockOrReport() {
        val state = BlockState()
        assertEquals(ThreadMenuEntry.BlockOrReport, state.menuEntry)
        assertTrue(state.showsBlock)
        assertFalse(state.showsUnblock)
        assertEquals(ComposerMode.Normal, composerMode(state, recipientUnavailable = false))
    }

    @Test
    fun blockedByMeShowsUnblockOrReport() {
        val state = BlockState().onBlockSucceeded()
        assertEquals(ThreadMenuEntry.UnblockOrReport, state.menuEntry)
        assertEquals(ComposerMode.BlockedByMe, composerMode(state, recipientUnavailable = false))
    }

    @Test
    fun latchedBlockedByMeMenuIsReportOnly() {
        assertEquals(ThreadMenuEntry.ReportOnly, BlockState().onBlockSucceeded().onNotBlocker().menuEntry)
    }

    @Test
    fun unavailableRecipientWinsOverNormalButNotOverBlock() {
        assertEquals(ComposerMode.Unavailable, composerMode(BlockState(), recipientUnavailable = true))
        val blocked = BlockState().onBlockSucceeded()
        assertEquals(ComposerMode.BlockedByMe, composerMode(blocked, recipientUnavailable = true))
    }
}
