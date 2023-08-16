package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class CardReaderPaymentsUtilsModule {
    @Provides
    fun providePaymentsUtils() = PaymentUtils
}
