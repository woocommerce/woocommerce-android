package com.woocommerce.android.cardreader.internal.config

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardReaderConfigFactoryTest : CardReaderBaseUnitTest() {
    private lateinit var cardReaderConfigFactory: CardReaderConfigFactory

    @Before
    fun setUp() {
        cardReaderConfigFactory = CardReaderConfigFactory()
    }

    @Test
    fun `given country code US, when getCardReaderConfigFor is called, then US card reader config  returned`() {
        // GIVEN
        val countryCode = "US"
        val expectedCardReaderConfig = CardReaderConfigForUSA

        // WHEN
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        // THEN
        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }

    @Test
    fun `given country code CA, when getCardReaderConfigFor is called, then Canada card reader config returned`() {
        // GIVEN
        val countryCode = "CA"
        val expectedCardReaderConfig = CardReaderConfigForCanada

        // WHEN
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        // THEN
        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }

    @Test
    fun `given unsupported country code, when getCardReaderConfigFor is called, then unsupported country card reader config returned`() {
        // GIVEN
        val countryCode = "invalid country code"
        val expectedCardReaderConfig = CardReaderConfigForUnsupportedCountry

        // WHEN
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        // THEN
        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }

    @Test
    fun `given PR country code, when getCardReaderConfigFor is called, then US card reader config returned`() {
        // GIVEN
        val countryCode = "PR"
        val expectedCardReaderConfig = CardReaderConfigForUSA

        // WHEN
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(countryCode)

        // THEN
        assertThat(cardReaderConfig).isInstanceOf(expectedCardReaderConfig::class.java)
    }
}
