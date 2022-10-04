package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUnsupportedCountry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardReaderCountryConfigProviderTest {
    private val cardReaderConfigFactory: CardReaderConfigFactory = mock()

    private val cardReaderCountryConfigProvider = CardReaderCountryConfigProvider(
        cardReaderConfigFactory,
    )

    @Test
    fun `given CA and factory returns canada, when config provide, then Canada returned`() {
        // GIVEN
        val countryCode = "CA"
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(countryCode)).thenReturn(CardReaderConfigForCanada)

        // WHEN
        val config = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)

        // THEN
        assertThat(config).isInstanceOf(CardReaderConfigForCanada::class.java)
    }

    @Test
    fun `given US and factory returns USA, when config provide, then USA returned`() {
        // GIVEN
        val countryCode = "US"
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(countryCode)).thenReturn(CardReaderConfigForUSA)

        // WHEN
        val config = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)

        // THEN
        assertThat(config).isInstanceOf(CardReaderConfigForUSA::class.java)
    }

    @Test
    fun `given RU and factory returns unsupported, when config provide, then unsupport returned`() {
        // GIVEN
        val countryCode = "RU"
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(countryCode)).thenReturn(
            CardReaderConfigForUnsupportedCountry
        )

        // WHEN
        val config = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)

        // THEN
        assertThat(config).isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
    }
}
