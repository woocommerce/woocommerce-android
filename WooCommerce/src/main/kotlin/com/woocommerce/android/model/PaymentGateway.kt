package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.gateways.WCGatewayModel

const val REFUNDS_FEATURE = "refunds"

data class PaymentGateway(
    val title: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val methodTitle: String,
    val methodDescription: String = "",
    val supportsRefunds: Boolean = false
)

fun WCGatewayModel.toAppModel(): PaymentGateway {
    return PaymentGateway(
            this.title,
            this.description,
            this.enabled,
            this.methodTitle,
            this.methodDescription,
            features.contains(REFUNDS_FEATURE)
    )
}
