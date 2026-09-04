package com.woocommerce.android.ui.woopos.tab

/**
 * The countries POS supports, and the store currencies allowed in each of them.
 *
 * This table mirrors the iOS `POSCountryCurrencyValidator` so both apps admit the same stores.
 * Keep the two in sync when a country is added or removed.
 */
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

    /**
     * The currencies POS supports in [countryCode], or an empty set when the country is not a
     * supported POS country and therefore has no currency to validate against.
     */
    fun currenciesFor(countryCode: String): Set<String> =
        CURRENCIES_BY_COUNTRY[countryCode.uppercase()].orEmpty()
}
