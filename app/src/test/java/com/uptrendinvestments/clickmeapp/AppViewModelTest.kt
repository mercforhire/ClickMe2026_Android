package com.uptrendinvestments.clickmeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelTest {
    @Test
    fun freshViewModelResolvesClientHomeSynchronously() {
        val viewModel = AppViewModel()

        assertEquals(AppRoute.ClientHome, viewModel.route.value)
        assertTrue(viewModel.destinationResolved.value)
    }

    @Test
    fun debugSwitchScaffoldTogglesBetweenClientAndExpertHome() {
        val viewModel = AppViewModel()

        viewModel.debugSwitchScaffold()
        assertEquals(AppRoute.ExpertHome, viewModel.route.value)

        viewModel.debugSwitchScaffold()
        assertEquals(AppRoute.ClientHome, viewModel.route.value)
    }

    @Test
    fun appRouteMirrorsIosRoutesInOrder() {
        assertEquals(
            listOf(
                AppRoute.Splash,
                AppRoute.Login,
                AppRoute.SignupSetup,
                AppRoute.ClientHome,
                AppRoute.ExpertHome,
            ),
            AppRoute.entries,
        )
    }
}
