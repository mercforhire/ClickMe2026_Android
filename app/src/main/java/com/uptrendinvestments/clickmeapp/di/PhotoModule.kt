package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.AndroidPhotoProcessor
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoWorkingFilesCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/** Binds the on-device profile photo pipeline, and registers its working files as session data (CR-03). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    @Singleton
    abstract fun bindPhotoProcessor(impl: AndroidPhotoProcessor): PhotoProcessor

    @Binds
    @IntoSet
    abstract fun bindPhotoWorkingFilesCleaner(impl: PhotoWorkingFilesCleaner): SessionDataCleaner
}
