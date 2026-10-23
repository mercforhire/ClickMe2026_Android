package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Payment Methods. One destination reached from the Profile tab row and from the booking gate
 * (D-04, D-08); registered once in the client shell (07-12).
 */
@Serializable
data object PaymentMethodsKey : NavKey
