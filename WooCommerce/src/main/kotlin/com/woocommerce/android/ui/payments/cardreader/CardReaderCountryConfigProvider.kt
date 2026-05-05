package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.config.CardReaderConfig
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import java.util.Locale
import javax.inject.Inject

class CardReaderCountryConfigProvider @Inject constructor(
    private val cardReaderConfigFactory: CardReaderConfigFactory,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    fun provideCountryConfigFor(countryCode: String?): CardReaderConfig {
        val raw = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
        if (raw is CardReaderConfigForUnsupportedCountry) return raw

        val gatingFlag = countryCode?.uppercase(Locale.ROOT)?.let(EXPANSION_COUNTRY_FEATURE_FLAGS::get)
            ?: return raw

        return if (featureFlagRepository.isEnabled(gatingFlag)) {
            raw
        } else {
            CardReaderConfigForUnsupportedCountry
        }
    }

    private companion object {
        val EXPANSION_COUNTRY_FEATURE_FLAGS = mapOf(
            "FR" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "DE" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "IE" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "NL" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "SG" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "NZ" to FeatureFlag.IPP_COUNTRY_EXPANSION,
            "AT" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "BE" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "FI" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "IT" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "LU" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "PT" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
            "ES" to FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED,
        )
    }
}
