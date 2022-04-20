package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.RefundParameters
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import java.math.BigDecimal

data class RefundParams(
    val chargeId: String,
    val amount: BigDecimal,
    val currency: String
)

internal fun RefundParams.toStripeRefundParameters(paymentUtils: PaymentUtils) = RefundParameters.Builder(
    chargeId = this.chargeId,
    amount = paymentUtils.convertBigDecimalInDollarsToLongInCents(this.amount),
    currency = this.currency
).build()
