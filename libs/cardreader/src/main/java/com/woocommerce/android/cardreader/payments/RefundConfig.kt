package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.CollectRefundConfiguration
import com.stripe.stripeterminal.external.models.CustomerCancellation

data class RefundConfig(
    val enableCustomerCancellation: Boolean
)

internal fun RefundConfig.toStripeRefundConfiguration(): CollectRefundConfiguration {
    return CollectRefundConfiguration.Builder()
        .setCustomerCancellation(
            if (enableCustomerCancellation) {
                CustomerCancellation.ENABLE_IF_AVAILABLE
            } else {
                CustomerCancellation.DISABLE_IF_AVAILABLE
            }
        )
        .build()
}
