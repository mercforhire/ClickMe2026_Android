package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Reschedule seam: every pending post-send hand-off slot is cleared on logout. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RescheduleModule {
    @Binds
    @IntoSet
    abstract fun bindRescheduleSentCleaner(impl: RescheduleSentRequest): SessionDataCleaner
}
