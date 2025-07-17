package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import javax.inject.Inject

class ValidateHSTariffNumber @Inject constructor() {
    operator fun invoke(tariffNumber: String): Boolean {
        val regex = Regex("""^(\d{1,2}\.?){3,6}$""")
        if (!regex.matches(tariffNumber)) return false

        val digits = tariffNumber.replace(Regex("""\D"""), "")
        return digits.length in 6..12
    }
}
