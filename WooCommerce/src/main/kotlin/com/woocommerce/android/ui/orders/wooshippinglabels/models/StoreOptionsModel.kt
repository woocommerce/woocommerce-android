package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
