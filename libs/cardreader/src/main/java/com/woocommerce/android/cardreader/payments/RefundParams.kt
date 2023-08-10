package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.RefundParameters
import com.woocommerce.android.cardreader.internal.payments.convertInCurrencyScaleOf2ToLongInCents
import java.math.BigDecimal

data class RefundParams(
    val chargeId: String,
    val amount: BigDecimal,
    val currency: String
)

internal fun RefundParams.toStripeRefundParameters() = RefundParameters.Builder(
    chargeId = this.chargeId,
    amount = this.amount.convertInCurrencyScaleOf2ToLongInCents(),
    currency = this.currency
).build()
