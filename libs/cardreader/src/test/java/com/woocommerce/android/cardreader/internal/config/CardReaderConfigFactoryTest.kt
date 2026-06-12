package com.woocommerce.android.cardreader.internal.config

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForAustralia
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForFI
import com.woocommerce.android.cardreader.config.CardReaderConfigForGB
import com.woocommerce.android.cardreader.config.CardReaderConfigForIE
import com.woocommerce.android.cardreader.config.CardReaderConfigForLU
import com.woocommerce.android.cardreader.config.CardReaderConfigForNL
import com.woocommerce.android.cardreader.config.CardReaderConfigForNZ
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
    fun `given country code GB, when getCardReaderConfigFor is called, then GB card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("GB"))
            .isInstanceOf(CardReaderConfigForGB::class.java)
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
    fun `given fiscalization country codes, when getCardReaderConfigFor is called, then unsupported returned`() {
        listOf("AT", "BE", "FR", "IT", "DE", "PT", "ES").forEach { countryCode ->
            assertThat(cardReaderConfigFactory.getCardReaderConfigFor(countryCode))
                .`as`("Expected $countryCode to be unsupported")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
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
    fun `given country code FI, when getCardReaderConfigFor is called, then Finland card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("FI"))
            .isInstanceOf(CardReaderConfigForFI::class.java)
    }

    @Test
    fun `given country code LU, when getCardReaderConfigFor is called, then Luxembourg card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("LU"))
            .isInstanceOf(CardReaderConfigForLU::class.java)
    }

    @Test
    fun `given country code AU, when getCardReaderConfigFor is called, then Australia card reader config returned`() {
        assertThat(cardReaderConfigFactory.getCardReaderConfigFor("AU"))
            .isInstanceOf(CardReaderConfigForAustralia::class.java)
    }
}
