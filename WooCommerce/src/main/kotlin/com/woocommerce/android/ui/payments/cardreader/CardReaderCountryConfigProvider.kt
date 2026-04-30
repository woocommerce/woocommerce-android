package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.config.CardReaderConfig
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class CardReaderCountryConfigProvider @Inject constructor(
    private val cardReaderConfigFactory: CardReaderConfigFactory,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    fun provideCountryConfigFor(countryCode: String?): CardReaderConfig {
        val raw = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
        if (raw is CardReaderConfigForUnsupportedCountry) return raw

        val normalised = countryCode?.uppercase()
        return when (normalised) {
            in PRIMARY_EXPANSION_COUNTRIES ->
                if (featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)) {
                    raw
                } else {
                    CardReaderConfigForUnsupportedCountry
                }
            in EU_EXTENDED_COUNTRIES ->
                if (featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)) {
                    raw
                } else {
                    CardReaderConfigForUnsupportedCountry
                }
            else -> raw
        }
    }

    private companion object {
        val PRIMARY_EXPANSION_COUNTRIES = setOf("FR", "DE", "IE", "NL", "SG", "NZ")
        val EU_EXTENDED_COUNTRIES = setOf("AT", "BE", "FI", "IT", "LU", "PT", "ES")
    }
}
