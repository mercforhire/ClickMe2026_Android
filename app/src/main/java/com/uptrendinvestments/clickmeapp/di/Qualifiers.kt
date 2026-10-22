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

/**
 * The `clickme_categories` Preferences DataStore that backs the categories cache. It is a separate file from the
 * unqualified session store, so it survives logout (D-08).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CategoryCache
