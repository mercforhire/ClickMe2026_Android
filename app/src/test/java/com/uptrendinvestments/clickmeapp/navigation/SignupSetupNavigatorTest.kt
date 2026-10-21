package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import org.junit.Assert.assertEquals
import org.junit.Test

class SignupSetupNavigatorTest {
    private fun newNavigator(): SignupSetupNavigator {
        val state =
            NavigationState(
                startRoute = HubKey,
                topLevelRoute = mutableStateOf<NavKey>(HubKey),
                backStacks = mapOf(HubKey to NavBackStack<NavKey>(HubKey)),
            )
        return SignupSetupNavigator(Navigator(state))
    }

    private fun SignupSetupNavigator.stack(): List<NavKey> {
        val hubStack = navigator.state.backStacks.getValue(HubKey)
        return hubStack.toList()
    }

    @Test
    fun openStepPushesStepOnTopOfHub() {
        val nav = newNavigator()
        nav.openStep(SetupStep.PersonalDetails)
        assertEquals(listOf<NavKey>(HubKey, PersonalDetailsKey), nav.stack())
    }

    @Test
    fun everyStepMapsToItsKey() {
        val nav = newNavigator()
        assertEquals(PersonalDetailsKey, nav.keyFor(SetupStep.PersonalDetails))
        assertEquals(TimezoneKey, nav.keyFor(SetupStep.Timezone))
        assertEquals(ProfilePhotoKey, nav.keyFor(SetupStep.ProfilePhoto))
        assertEquals(VerifyEmailKey, nav.keyFor(SetupStep.VerifyEmail))
        assertEquals(TagsKey, nav.keyFor(SetupStep.Expertise))
    }

    @Test
    fun openStepTwiceIsDoubleTapGuarded() {
        val nav = newNavigator()
        nav.openStep(SetupStep.Timezone)
        nav.openStep(SetupStep.Timezone)
        assertEquals(listOf<NavKey>(HubKey, TimezoneKey), nav.stack())
    }

    @Test
    fun openReviewThenEditStepAndBackLandsOnReview() {
        val nav = newNavigator()
        nav.openReview()
        nav.openStep(SetupStep.Expertise)
        assertEquals(listOf<NavKey>(HubKey, ReviewKey, TagsKey), nav.stack())
        nav.back()
        assertEquals(listOf<NavKey>(HubKey, ReviewKey), nav.stack())
    }

    @Test
    fun openCropPushesOnPhotoStepAndBackReturnsToIt() {
        val nav = newNavigator()
        nav.openStep(SetupStep.ProfilePhoto)
        nav.openCrop()
        assertEquals(listOf<NavKey>(HubKey, ProfilePhotoKey, PhotoCropKey), nav.stack())
        assertEquals(PhotoCropKey, nav.top)
        nav.back()
        assertEquals(listOf<NavKey>(HubKey, ProfilePhotoKey), nav.stack())
    }

    @Test
    fun returnToHubClearsEverythingAboveHub() {
        val nav = newNavigator()
        nav.openReview()
        nav.openStep(SetupStep.Expertise)
        nav.returnToHub()
        assertEquals(listOf<NavKey>(HubKey), nav.stack())
    }

    @Test
    fun backAtHubRootIsNoOp() {
        val nav = newNavigator()
        nav.back()
        assertEquals(listOf<NavKey>(HubKey), nav.stack())
    }

    @Test
    fun openReviewTwiceIsDoubleTapGuarded() {
        val nav = newNavigator()
        nav.openReview()
        nav.openReview()
        assertEquals(listOf<NavKey>(HubKey, ReviewKey), nav.stack())
    }
}
