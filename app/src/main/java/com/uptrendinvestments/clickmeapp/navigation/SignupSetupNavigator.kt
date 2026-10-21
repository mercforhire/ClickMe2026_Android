package com.uptrendinvestments.clickmeapp.navigation

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep

/**
 * Setup-flow navigation actions over [Navigator]. The setup stack is a single stack rooted at [HubKey];
 * steps, the crop screen and Review are pushed on it. A step's Save pops back to wherever it was opened
 * (hub or Review), so Review's Edit buttons reuse the same steps (SIGN-02).
 */
class SignupSetupNavigator(
    val navigator: Navigator,
) {
    /** The key currently on top of the setup stack. */
    val top: NavKey
        get() {
            val hubStack = navigator.state.backStacks.getValue(HubKey)
            return hubStack.last()
        }

    fun openStep(step: SetupStep) = pushUnlessOnTop(keyFor(step))

    fun openReview() = pushUnlessOnTop(ReviewKey)

    fun openCrop() = pushUnlessOnTop(PhotoCropKey)

    /** Pops one entry; at the hub root this is a no-op and system Back exits the app. */
    fun back() = navigator.goBack()

    /** Pops everything above the hub (D-06: OK on the missing-fields dialog). */
    fun returnToHub() = navigator.reselect(HubKey)

    fun keyFor(step: SetupStep): NavKey =
        when (step) {
            SetupStep.PersonalDetails -> PersonalDetailsKey
            SetupStep.Timezone -> TimezoneKey
            SetupStep.ProfilePhoto -> ProfilePhotoKey
            SetupStep.VerifyEmail -> VerifyEmailKey
            SetupStep.Expertise -> TagsKey
        }

    /** Double-tap guard: no-op when the same key type is already on top. */
    private fun pushUnlessOnTop(key: NavKey) {
        if (top::class != key::class) navigator.navigate(key)
    }
}
