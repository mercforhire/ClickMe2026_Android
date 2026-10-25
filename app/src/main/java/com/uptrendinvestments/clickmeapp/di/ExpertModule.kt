package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicEvents
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Registers the saved-topic channel as session data, so an unconsumed event is dropped on logout and login. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExpertModule {
    @Binds
    @IntoSet
    abstract fun bindTopicEventsCleaner(impl: TopicEvents): SessionDataCleaner
}
