package com.uptrendinvestments.clickmeapp.di

import com.uptrendinvestments.clickmeapp.stripe.DefaultStripePaymentMethodReader
import com.uptrendinvestments.clickmeapp.stripe.StripePaymentMethodReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the SetupIntent `pm_` reader so ViewModels depend on the interface and never on the Stripe SDK. */
@Module
@InstallIn(SingletonComponent::class)
abstract class StripeModule {
    @Binds
    abstract fun bindStripePaymentMethodReader(impl: DefaultStripePaymentMethodReader): StripePaymentMethodReader
}
