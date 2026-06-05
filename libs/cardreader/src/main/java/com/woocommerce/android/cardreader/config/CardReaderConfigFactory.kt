package com.woocommerce.android.cardreader.config

class CardReaderConfigFactory {
    fun getCardReaderConfigFor(countryCode: String?): CardReaderConfig =
        COUNTRY_CONFIGS[countryCode] ?: CardReaderConfigForUnsupportedCountry

    private companion object {
        val COUNTRY_CONFIGS: Map<String, CardReaderConfigForSupportedCountry> = mapOf(
            // PR is Puerto Rico and it's a US territory
            "US" to CardReaderConfigForUSA,
            "PR" to CardReaderConfigForUSA,
            "CA" to CardReaderConfigForCanada,
            "GB" to CardReaderConfigForGB,
            "IE" to CardReaderConfigForIE,
            "NL" to CardReaderConfigForNL,
            "SG" to CardReaderConfigForSG,
            "NZ" to CardReaderConfigForNZ,
            "FI" to CardReaderConfigForFI,
            "LU" to CardReaderConfigForLU,
            "AU" to CardReaderConfigForAustralia,
        )
    }
}
