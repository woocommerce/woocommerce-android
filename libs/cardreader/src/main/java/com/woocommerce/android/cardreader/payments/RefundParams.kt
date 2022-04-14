package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.RefundParameters
import java.math.BigDecimal

data class RefundParams(
    val chargeId: String,
    val amount: BigDecimal,
    val currency: String
)

fun RefundParams.toStripeRefundParameters() = RefundParameters.Builder(
    chargeId = this.chargeId,
    amount = this.amount.toLong(),
    currency = this.currency
).build()
