package com.woocommerce.android.ui.orders.creation.taxes.rates

import com.woocommerce.android.extensions.isNotNullOrEmpty
import javax.inject.Inject

class GetTaxRateLabel @Inject constructor() {
    @Suppress("ComplexCondition")
    operator fun invoke(taxRate: TaxRate) = StringBuilder().apply {
        if (taxRate.name.isNotNullOrEmpty()) {
            append(taxRate.name)
        }
        if (taxRate.countryCode.isNotNullOrEmpty() ||
            taxRate.stateCode.isNotNullOrEmpty() ||
            taxRate.postcode.isNotNullOrEmpty() ||
            taxRate.city.isNotNullOrEmpty()
        ) {
            append(" · ")
        }
        if (taxRate.countryCode.isNotNullOrEmpty()) {
            append(taxRate.countryCode)
            append(SPACE_CHAR)
        }
        if (taxRate.stateCode.isNotNullOrEmpty()) {
            append(taxRate.stateCode)
            append(SPACE_CHAR)
        }
        if (taxRate.postcode.isNotNullOrEmpty()) {
            append(taxRate.postcode)
            append(SPACE_CHAR)
        }
        if (taxRate.city.isNotNullOrEmpty()) {
            append(taxRate.city)
        }
    }.toString()

    private companion object {
        private const val SPACE_CHAR = " "
    }
}