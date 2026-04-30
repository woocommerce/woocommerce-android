package com.woocommerce.android.cardreader.internal.config

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForAT
import com.woocommerce.android.cardreader.config.CardReaderConfigForBE
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForDE
import com.woocommerce.android.cardreader.config.CardReaderConfigForES
import com.woocommerce.android.cardreader.config.CardReaderConfigForFI
import com.woocommerce.android.cardreader.config.CardReaderConfigForFR
import com.woocommerce.android.cardreader.config.CardReaderConfigForIE
import com.woocommerce.android.cardreader.config.CardReaderConfigForIT
import com.woocommerce.android.cardreader.config.CardReaderConfigForLU
import com.woocommerce.android.cardreader.config.CardReaderConfigForNL
import com.woocommerce.android.cardreader.config.CardReaderConfigForNZ
import com.woocommerce.android.cardreader.config.CardReaderConfigForPT
import com.woocommerce.android.cardreader.config.CardReaderConfigForSG
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

    @Test
    fun `given country code FR, when getCardReaderConfigFor is called, then France card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("FR"))
            .isInstanceOf(CardReaderConfigForFR::class.java)
    }

    @Test
    fun `given country code DE, when getCardReaderConfigFor is called, then Germany card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("DE"))
            .isInstanceOf(CardReaderConfigForDE::class.java)
    }

    @Test
    fun `given country code IE, when getCardReaderConfigFor is called, then Ireland card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("IE"))
            .isInstanceOf(CardReaderConfigForIE::class.java)
    }

    @Test
    fun `given country code NL, when getCardReaderConfigFor is called, then Netherlands card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("NL"))
            .isInstanceOf(CardReaderConfigForNL::class.java)
    }

    @Test
    fun `given country code SG, when getCardReaderConfigFor is called, then Singapore card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("SG"))
            .isInstanceOf(CardReaderConfigForSG::class.java)
    }

    @Test
    fun `given country code NZ, when getCardReaderConfigFor is called, then New Zealand card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("NZ"))
            .isInstanceOf(CardReaderConfigForNZ::class.java)
    }

    @Test
    fun `given country code AT, when getCardReaderConfigFor is called, then Austria card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("AT"))
            .isInstanceOf(CardReaderConfigForAT::class.java)
    }

    @Test
    fun `given country code BE, when getCardReaderConfigFor is called, then Belgium card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("BE"))
            .isInstanceOf(CardReaderConfigForBE::class.java)
    }

    @Test
    fun `given country code FI, when getCardReaderConfigFor is called, then Finland card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("FI"))
            .isInstanceOf(CardReaderConfigForFI::class.java)
    }

    @Test
    fun `given country code IT, when getCardReaderConfigFor is called, then Italy card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("IT"))
            .isInstanceOf(CardReaderConfigForIT::class.java)
    }

    @Test
    fun `given country code LU, when getCardReaderConfigFor is called, then Luxembourg card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("LU"))
            .isInstanceOf(CardReaderConfigForLU::class.java)
    }

    @Test
    fun `given country code PT, when getCardReaderConfigFor is called, then Portugal card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("PT"))
            .isInstanceOf(CardReaderConfigForPT::class.java)
    }

    @Test
    fun `given country code ES, when getCardReaderConfigFor is called, then Spain card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("ES"))
            .isInstanceOf(CardReaderConfigForES::class.java)
    }

    @Test
    fun `given country code AU, when getCardReaderConfigFor is called, then unsupported config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("AU"))
            .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
    }
}
