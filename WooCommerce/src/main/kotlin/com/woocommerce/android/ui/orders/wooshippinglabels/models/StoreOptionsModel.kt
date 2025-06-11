package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class AccountSettingsModel(
    val storeOptions: StoreOptionsModel,
    val paymentMethods: List<PaymentMethodModel>
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

data class PaymentMethodModel(
    val paymentMethodId: Int,
    val name: String,
    val cardType: String,
    val cardDigits: String,
    val expiry: String
)
