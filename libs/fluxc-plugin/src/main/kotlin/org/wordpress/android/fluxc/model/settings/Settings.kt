package org.wordpress.android.fluxc.model.settings

data class Settings(
    val currencyCode: String,
    val currencyPosition: CurrencyPosition,
    val currencyThousandSeparator: String,
    val currencyDecimalSeparator: String,
    val currencyDecimalNumber: Int,
    val countryCode: String,
    val stateCode: String,
    val address: String,
    val address2: String,
    val city: String,
    val postalCode: String,
    val couponsEnabled: Boolean
)
