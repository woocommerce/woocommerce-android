package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import javax.inject.Inject

class CardReaderCountryConfigProvider @Inject constructor(
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    fun provideCountryConfigFor(countryCode: String?) = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
}
