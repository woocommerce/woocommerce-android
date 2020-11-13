package com.woocommerce.android.util

import java.util.Locale

// TODO: soon to be deprecated
object AddressUtils {
    /**
     * Translates a two-character country code into a human
     * readable label.
     *
     * Example: US -> United States
     */
    fun getCountryLabelByCountryCode(countryCode: String): String {
        val locale = Locale(Locale.getDefault().language, countryCode)
        return locale.displayCountry
    }
}
