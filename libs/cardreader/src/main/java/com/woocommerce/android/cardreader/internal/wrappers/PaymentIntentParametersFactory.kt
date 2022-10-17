package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.PaymentMethodType

internal class PaymentIntentParametersFactory {
    fun createBuilder(
        paymentMethodType: List<PaymentMethodType> = listOf(PaymentMethodType.CARD_PRESENT)
    ) = PaymentIntentParameters.Builder(paymentMethodType)
}
