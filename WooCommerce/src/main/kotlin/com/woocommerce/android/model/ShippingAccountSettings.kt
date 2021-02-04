package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingAccountSettings

data class ShippingAccountSettings(
    val canManagePayments: Boolean,
    val selectedPaymentId: Int?,
    val paymentMethods: List<PaymentMethod>,
    val lastUsedBoxId: String?
)

data class PaymentMethod(
    val id: Int,
    val name: String,
    val cardType: String,
    val cardDigits: String
)

fun WCShippingAccountSettings.toAppModel(): ShippingAccountSettings {
    return ShippingAccountSettings(
        canManagePayments = canManagePayments,
        selectedPaymentId = selectedPaymentMethodId,
        paymentMethods = paymentMethods.map {
            PaymentMethod(
                id = it.paymentMethodId,
                name = it.name,
                cardType = it.cardType,
                cardDigits = it.cardDigits
            )
        },
        lastUsedBoxId = lastUsedBoxId
    )
}
