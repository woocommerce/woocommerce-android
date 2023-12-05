package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.RefundConfiguration

data class RefundConfig(
    val enableCustomerCancellation: Boolean
)

internal fun RefundConfig.toStripeRefundConfiguration(): RefundConfiguration {
    return RefundConfiguration.Builder()
        .setEnableCustomerCancellation(this.enableCustomerCancellation)
        .build()
}
