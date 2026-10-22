package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.CompletedBookingStore
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.DataStoreCompletedBookingStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Binds the durable completed-booking record (WR-01) and registers it as session data, so logout clears it. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BookingModule {
    @Binds
    abstract fun bindCompletedBookingStore(impl: DataStoreCompletedBookingStore): CompletedBookingStore

    @Binds
    @IntoSet
    abstract fun bindCompletedBookingCleaner(impl: DataStoreCompletedBookingStore): SessionDataCleaner
}
