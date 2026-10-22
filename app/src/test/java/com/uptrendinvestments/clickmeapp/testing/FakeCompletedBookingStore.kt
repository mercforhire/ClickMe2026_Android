package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.CompletedBookingStore

/**
 * In-memory [CompletedBookingStore]. Share one instance between two ViewModels to model the on-disk record that
 * outlives a process death; the records map plays the part of the file.
 */
class FakeCompletedBookingStore : CompletedBookingStore {
    val records = mutableMapOf<String, String>()
    val discarded = mutableListOf<String>()

    override suspend fun put(
        attemptId: String,
        bookingId: String,
    ) {
        records.clear()
        records[attemptId] = bookingId
    }

    override suspend fun get(attemptId: String): String? = records[attemptId]

    override fun discard(attemptId: String) {
        discarded += attemptId
        records.remove(attemptId)
    }
}
