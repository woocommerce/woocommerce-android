package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.RefundParameters.Id
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import java.math.BigDecimal

data class RefundParams(
    val chargeId: String,
    val amount: BigDecimal,
    val currency: String
)

internal fun RefundParams.toStripeRefundParameters(paymentUtils: PaymentUtils): RefundParameters {
    return RefundParameters.Builder(
        Id.Charge(id = this.chargeId),
        amount = paymentUtils.convertToSmallestCurrencyUnit(this.amount, this.currency),
        currency = this.currency
    ).build()
}
