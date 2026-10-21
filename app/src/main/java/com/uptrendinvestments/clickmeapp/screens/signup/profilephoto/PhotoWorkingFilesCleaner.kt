package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import javax.inject.Inject

/** Deletes the signup photo working files when a session ends or a new one starts (T-04-18, CR-03). */
class PhotoWorkingFilesCleaner
    @Inject
    constructor(
        private val photoProcessor: PhotoProcessor,
    ) : SessionDataCleaner {
        override suspend fun clearSessionData() = photoProcessor.clearWorkingFiles()
    }
