package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.external.models.PaymentIntentParameters

internal class PaymentIntentParametersFactory {
    fun createBuilder() = PaymentIntentParameters.Builder()
}
