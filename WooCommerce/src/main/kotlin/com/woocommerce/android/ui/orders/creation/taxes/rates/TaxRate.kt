package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaxRate(
    val id: Int,
    val countryCode: String = "",
    val stateCode: String = "",
    val postcode: String = "",
    val city: String = "",
    val postCodes: List<String>? = null,
    val cities: List<String>? = null,
    val rate: String = "",
    val name: String = "",
    val priority: Int = 0,
    val compound: Boolean = false,
    val shipping: Boolean = false,
    val order: Int = 0,
    val taxClass: String = "",
) : Parcelable
