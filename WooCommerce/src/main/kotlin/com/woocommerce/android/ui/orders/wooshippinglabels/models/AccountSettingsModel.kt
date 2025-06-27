package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.woocommerce.android.extensions.capitalize
import kotlinx.parcelize.Parcelize

data class AccountSettingsModel(
    val storeOptions: StoreOptionsModel,
    val paymentMethodOptions: PaymentMethodOptions,
    val canManagePayments: Boolean,
    val canEditSettings: Boolean,
    val storeOwnerName: String,
    val storeOwnerUsername: String
)

@Parcelize
data class StoreOptionsModel(
    val currencySymbol: String,
    val dimensionUnit: String,
    val weightUnit: String,
    val originCountry: String
) : Parcelable {
    companion object {
        val EMPTY = StoreOptionsModel(
            currencySymbol = "",
            dimensionUnit = "",
            weightUnit = "",
            originCountry = ""
        )
    }
}

data class PaymentMethodOptions(
    val selectedPaymentId: Long?,
    val paymentMethods: List<PaymentMethodModel>,
    val addPaymentMethodUrl: String,
    val emailReceipts: Boolean
) {
    val selectedPaymentMethod: PaymentMethodModel?
        get() = paymentMethods.find { it.paymentMethodId == selectedPaymentId }
}

data class PaymentMethodModel(
    val paymentMethodId: Long,
    val name: String,
    val cardType: String,
    val cardDigits: String,
    val expiry: String
) {
    val cardTypeWithDigits: String
        get() {
            val cardTypeFormatted = when (cardType.lowercase()) {
                "visa" -> "VISA"
                "mastercard" -> "MasterCard"
                "amex" -> "American Express"
                else -> cardType.capitalize()
            }
            return "$cardTypeFormatted****$cardDigits"
        }
}
