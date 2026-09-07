package com.woocommerce.android.ui.woopos.tab

/** Mirrors the iOS `POSCountryCurrencyValidator`. Keep the two in sync. */
object WooPosSupportedCountries {
    private val CURRENCIES_BY_COUNTRY: Map<String, Set<String>> = mapOf(
        "US" to setOf("USD"),
        "PR" to setOf("USD"),
        "GB" to setOf("GBP"),
        "CA" to setOf("CAD"),
        "FI" to setOf("EUR"),
        "IE" to setOf("EUR"),
        "LU" to setOf("EUR"),
        "NL" to setOf("EUR"),
        "SG" to setOf("SGD"),
        "NZ" to setOf("NZD"),
        "AU" to setOf("AUD"),
    )

    val countryCodes: Set<String> = CURRENCIES_BY_COUNTRY.keys

    fun isSupported(countryCode: String): Boolean = countryCode.uppercase() in CURRENCIES_BY_COUNTRY

    fun currenciesFor(countryCode: String): Set<String> =
        CURRENCIES_BY_COUNTRY[countryCode.uppercase()].orEmpty()
}
