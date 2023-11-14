package com.woocommerce.android.cardreader.internal.wrappers

import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType

internal class PaymentIntentParametersFactory(
    private val mapper: PaymentMethodTypeMapper
) {
    fun createBuilder(
        paymentMethodType: List<PaymentMethodType> = listOf(PaymentMethodType.CARD_PRESENT)
    ) = PaymentIntentParameters.Builder(paymentMethodType.map { mapper.map(it) })
}
