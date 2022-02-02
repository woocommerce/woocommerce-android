package com.woocommerce.android.cardreader.internal.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CardReaderConfigFactoryTest {
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

    @Test(expected = IllegalStateException::class)
    fun `given invalid country code, then exception is thrown`() {
        val countryCode = "invalid country code"

        cardReaderConfigFactory.getCardReaderConfigFor(countryCode)
    }
}
