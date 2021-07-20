package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.external.models.PaymentIntentParameters

class PaymentIntentParametersFactory {
    fun createBuilder() = PaymentIntentParameters.Builder()
}
