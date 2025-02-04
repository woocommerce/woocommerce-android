package com.woocommerce.android.ui.orders.wooshippinglabels.models

data class StoreOptionsModel(
    val currencySymbol: String,
    val dimensionUnit: String,
    val weightUnit: String,
    val originCountry: String
) {
    companion object {
        val EMPTY = StoreOptionsModel(
            currencySymbol = "",
            dimensionUnit = "",
            weightUnit = "",
            originCountry = ""
        )
    }
}
