package com.uptrendinvestments.clickmeapp.di

import android.os.SystemClock
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Provides the production [MonotonicClock] and the wall-clock [Clock]. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    /** Monotonic time for in-process intervals only (never persisted, never compared to calendar dates). */
    @Provides
    @Singleton
    fun provideMonotonicClock(): MonotonicClock = MonotonicClock { SystemClock.elapsedRealtime() }

    /**
     * Wall time for values stored on disk or compared to calendar dates (CategoryStore TTL in D-07, Today's
     * Sessions in D-01).
     */
    @Provides
    @Singleton
    fun provideWallClock(): Clock = Clock.systemUTC()
}
