package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import java.math.BigDecimal
import javax.inject.Inject

class ShouldRequireITN @Inject constructor() {
    operator fun invoke(
        destinationCountryCode: String,
        totalShippingItemValue: BigDecimal
    ) = when (destinationCountryCode) {
        in itnExemptCountries -> false
        in itnRequiredCountries -> true
        else -> totalShippingItemValue > MAX_SHIPPING_ITEM_VALUE_FOR_CUSTOMS
    }

    private val itnExemptCountries = listOf("CA")
    private val itnRequiredCountries = listOf("IR", "KP", "SY", "CU", "SD")

    companion object {
        private val MAX_SHIPPING_ITEM_VALUE_FOR_CUSTOMS = BigDecimal.valueOf(2500)
    }
}
