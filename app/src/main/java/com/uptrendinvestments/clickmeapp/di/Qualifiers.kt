package com.uptrendinvestments.clickmeapp.di

import javax.inject.Qualifier

/** The IO [kotlinx.coroutines.CoroutineDispatcher] (`Dispatchers.IO`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The process-lifetime [kotlinx.coroutines.CoroutineScope] for work that must outlive any screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
