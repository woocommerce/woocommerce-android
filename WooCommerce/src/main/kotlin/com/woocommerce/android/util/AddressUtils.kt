package com.woocommerce.android.util

import java.util.Locale

object AddressUtils {
    fun getCountryLabelByCountryCode(countryCode: String): String {
        val locale = Locale(Locale.getDefault().language, countryCode)
        return locale.displayCountry
    }
}
