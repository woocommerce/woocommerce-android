package com.woocommerce.android.cardreader.internal.config

import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CardReaderConfigFactoryTest : CardReaderBaseUnitTest() {
    private lateinit var cardReaderConfigFactory: CardReaderConfigFactory

    @Before
    fun setUp() {
        cardReaderConfigFactory = CardReaderConfigFactory()
    }

    @Test
    fun `given country code US, then US card reader config is returned`() {
        val countryCode = "US"
        val expectedCardReaderConfig = CardReaderConfigForUSA

        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }

    @Test
    fun `given country code CA, then Canada card reader config is returned`() {
        val countryCode = "CA"
        val expectedCardReaderConfig = CardReaderConfigForCanada

        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }

    @Test
    fun `given unsupported country code, then unsupported country card reader config is returned`() {
        val countryCode = "invalid country code"
        val expectedCardReaderConfig = CardReaderConfigForUnsupportedCountry

        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }
}
