package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.AndroidPhotoProcessor
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the on-device profile photo pipeline. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    @Singleton
    abstract fun bindPhotoProcessor(impl: AndroidPhotoProcessor): PhotoProcessor
}
