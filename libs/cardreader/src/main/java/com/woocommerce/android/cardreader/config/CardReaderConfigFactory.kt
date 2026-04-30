package com.woocommerce.android.cardreader.config

class CardReaderConfigFactory {
    fun getCardReaderConfigFor(countryCode: String?): CardReaderConfig {
        return when (countryCode) {
            // PR is Puerto Rico and it's a US territory
            "US", "PR" -> CardReaderConfigForUSA
            "CA" -> CardReaderConfigForCanada
            "GB" -> CardReaderConfigForGB
            "FR" -> CardReaderConfigForFR
            "DE" -> CardReaderConfigForDE
            "IE" -> CardReaderConfigForIE
            "NL" -> CardReaderConfigForNL
            "SG" -> CardReaderConfigForSG
            "NZ" -> CardReaderConfigForNZ
            "AT" -> CardReaderConfigForAT
            "BE" -> CardReaderConfigForBE
            "FI" -> CardReaderConfigForFI
            "IT" -> CardReaderConfigForIT
            "LU" -> CardReaderConfigForLU
            "PT" -> CardReaderConfigForPT
            "ES" -> CardReaderConfigForES
            else -> CardReaderConfigForUnsupportedCountry
        }
    }
}
