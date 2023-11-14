package com.woocommerce.android.cardreader.internal.wrappers

import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.stripe.stripeterminal.external.models.PaymentMethodType as StripePaymentMethodType

internal class PaymentMethodTypeMapper {
    fun map(paymentMethodType: PaymentMethodType) = when (paymentMethodType) {
        PaymentMethodType.CARD_PRESENT -> StripePaymentMethodType.CARD_PRESENT
        PaymentMethodType.INTERAC_PRESENT -> StripePaymentMethodType.INTERAC_PRESENT
        PaymentMethodType.UNKNOWN -> error("Unknown payment method type")
    }
}
