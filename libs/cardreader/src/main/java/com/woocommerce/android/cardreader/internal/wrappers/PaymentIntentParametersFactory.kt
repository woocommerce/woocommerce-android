package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.model.external.PaymentIntentParameters

internal class PaymentIntentParametersFactory {
    fun createBuilder() = PaymentIntentParameters.Builder()
}
