package com.uptrendinvestments.clickmeapp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * Nav keys of the signup setup back stack under `AppRoute.SignupSetup`. None carries data: the draft
 * (names, photo, tags) lives in SignupSetupRepository, so process death restores the stack without
 * putting PII or URIs in saved navigation state (T-04-49).
 */

/** Overview hub, the stack root (D-05). */
@Serializable
data object HubKey : NavKey

/** Personal details step. */
@Serializable
data object PersonalDetailsKey : NavKey

/** Timezone step. */
@Serializable
data object TimezoneKey : NavKey

/** Profile photo step. */
@Serializable
data object ProfilePhotoKey : NavKey

/** Full-screen photo crop pushed from the profile photo step (D-12). */
@Serializable
data object PhotoCropKey : NavKey

/** Verify email step. */
@Serializable
data object VerifyEmailKey : NavKey

/** Expertise tags step. */
@Serializable
data object TagsKey : NavKey

/** Review and Publish. */
@Serializable
data object ReviewKey : NavKey
