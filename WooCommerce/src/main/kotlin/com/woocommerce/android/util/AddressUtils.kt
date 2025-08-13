package com.woocommerce.android.util

import java.util.Locale

object AddressUtils {
    /**
     * Translates a two-character country code into a human
     * readable label.
     *
     * Example: US -> United States
     */
    fun getCountryLabelByCountryCode(countryCode: String): String {
        val locale = Locale.Builder()
            .setLanguage(Locale.getDefault().language)
            .setRegion(countryCode)
            .build()
        return locale.displayCountry
    }
}
