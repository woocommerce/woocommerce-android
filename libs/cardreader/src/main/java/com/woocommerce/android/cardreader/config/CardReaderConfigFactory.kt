package com.woocommerce.android.cardreader.config

class CardReaderConfigFactory {
    fun getCardReaderConfigFor(countryCode: String?): CardReaderConfig {
        return when (countryCode) {
            // PR is Puerto Rico and it's a US territory
            "US", "PR" -> CardReaderConfigForUSA
            "CA" -> CardReaderConfigForCanada
            "GB" -> CardReaderConfigForGB
            else -> CardReaderConfigForUnsupportedCountry
        }
    }
}
