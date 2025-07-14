package com.woocommerce.android.ui.blaze.creation.payment

import com.woocommerce.android.ui.blaze.BlazeRepository

val BlazeRepository.PaymentMethod.subtitle: String?
    get() = when (info) {
        is BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard -> info.cardHolderName
        else -> null
    }
