package com.woocommerce.android.cardreader.config

class CardReaderConfigFactory {
    fun getCardReaderConfigFor(countryCode: String?): CardReaderConfig {
        return when (countryCode) {
            "US" -> CardReaderConfigForUSA
            "CA" -> CardReaderConfigForCanada
            "GB" -> CardReaderConfigForGB
            else -> CardReaderConfigForUnsupportedCountry
        }
    }
}
