package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.settings.CurrencyPosition

data class WCSettingsModel(
    val localSiteId: Int,
    val currencyCode: String, // The currency code for the site in 3-letter ISO 4217 format
    val currencyPosition: CurrencyPosition, // Where the currency symbol should be placed
    val currencyThousandSeparator: String, // The thousands separator character (e.g. the comma in 3,000)
    val currencyDecimalSeparator: String, // The decimal separator character (e.g. the dot in 41.12)
    val currencyDecimalNumber: Int, // How many decimal points to display
    val countryCode: String = "", // The country code for the site in 2-letter format i.e. US
    val address: String = "",
    val address2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val stateCode: String = "", // The state code for the site in 2-letter format i.e. NY
    val couponsEnabled: Boolean = false
)
