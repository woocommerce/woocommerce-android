package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.cardreader.internal.config.CardReaderConfig
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUnsupportedCountry
import javax.inject.Inject

class CardReaderCountryConfigProvider @Inject constructor(
    private val canadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag,
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    fun provideCountryConfigFor(countryCode: String?): CardReaderConfig {
        val config = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
        return if (config is CardReaderConfigForCanada && !canadaFeatureFlag.isEnabled()) {
            CardReaderConfigForUnsupportedCountry
        } else {
            config
        }
    }
}
