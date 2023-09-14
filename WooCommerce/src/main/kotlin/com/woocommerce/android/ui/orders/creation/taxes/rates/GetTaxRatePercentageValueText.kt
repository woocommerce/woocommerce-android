package com.woocommerce.android.ui.orders.creation.taxes.rates

import com.woocommerce.android.extensions.isNotNullOrEmpty
import java.math.RoundingMode
import javax.inject.Inject

class GetTaxRatePercentageValueText @Inject constructor() {
    operator fun invoke(taxRate: TaxRate) = if (taxRate.rate.isNotNullOrEmpty()) {
        val standardisedRate = taxRate.rate.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        "$standardisedRate%"
    } else {
        ""
    }
}