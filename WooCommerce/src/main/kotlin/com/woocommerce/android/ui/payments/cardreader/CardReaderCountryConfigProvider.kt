package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class CardReaderCountryConfigProvider @Inject constructor(
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    fun provideCountryConfigFor(countryCode: String?) =
        if (!isCardReaderIPPEnabledByFeatureFlagFor(countryCode)) {
            CardReaderConfigForUnsupportedCountry
        } else {
            cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
        }

    private fun isCardReaderIPPEnabledByFeatureFlagFor(countryCode: String?): Boolean {
        if (countryCode == "GB" && !FeatureFlag.IPP_UK.isEnabled()) return false

        return true
    }
}
