package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.CompletedBookingLookup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.CompletedBookingStore

/**
 * In-memory [CompletedBookingStore]. Share one instance between two ViewModels to model the on-disk record that
 * outlives a process death; the records map plays the part of the file. Set [unreadable] to model a read failure.
 */
class FakeCompletedBookingStore : CompletedBookingStore {
    val records = mutableMapOf<String, String>()
    val discarded = mutableListOf<String>()
    var unreadable = false

    override suspend fun put(
        attemptId: String,
        bookingId: String,
    ) {
        records.clear()
        records[attemptId] = bookingId
    }

    override suspend fun get(attemptId: String): CompletedBookingLookup =
        when {
            unreadable -> CompletedBookingLookup.Unreadable
            else -> records[attemptId]?.let(CompletedBookingLookup::Found) ?: CompletedBookingLookup.Absent
        }

    override fun discard(attemptId: String) {
        discarded += attemptId
        records.remove(attemptId)
    }
}
