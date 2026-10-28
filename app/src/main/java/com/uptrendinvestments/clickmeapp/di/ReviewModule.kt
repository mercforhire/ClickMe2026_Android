package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Reviews seams: the post-submit hand-off slots and the session's reviewed-booking set are cleared on logout. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewModule {
    @Binds
    @IntoSet
    abstract fun bindReviewLaunchCleaner(impl: ReviewLaunchRequest): SessionDataCleaner
}
