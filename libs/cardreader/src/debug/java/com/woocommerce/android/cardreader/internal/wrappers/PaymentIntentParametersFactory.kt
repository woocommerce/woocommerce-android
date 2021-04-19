package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.model.external.PaymentIntentParameters

class PaymentIntentParametersFactory {
    fun createBuilder() = PaymentIntentParameters.Builder()
}
