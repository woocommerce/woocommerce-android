package com.woocommerce.android.cardreader.internal.config

class CardReaderConfigFactory {
    fun getCardReaderConfigFor(countryCode: String?): CardReaderConfig {
        return when (countryCode) {
            "US" -> {
                CardReaderConfigForUSA
            }
            "CA" -> {
                CardReaderConfigForCanada
            }
            else -> {
                throw IllegalStateException("Invalid country code")
            }
        }
    }
}
