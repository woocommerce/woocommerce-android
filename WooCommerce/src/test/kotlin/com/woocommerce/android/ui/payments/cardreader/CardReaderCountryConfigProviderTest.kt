package com.woocommerce.android.ui.payments.cardreader

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardReaderCountryConfigProviderTest {
    private val cardReaderConfigFactory: CardReaderConfigFactory = CardReaderConfigFactory()
    private val sut = CardReaderCountryConfigProvider(cardReaderConfigFactory)

    @Test
    fun `given CA country code, when config provide, then Canada returned`() {
        assertThat(sut.provideCountryConfigFor("CA"))
            .isInstanceOf(CardReaderConfigForCanada::class.java)
    }

    @Test
    fun `given US country code, when config provide, then USA returned`() {
        assertThat(sut.provideCountryConfigFor("US"))
            .isInstanceOf(CardReaderConfigForUSA::class.java)
    }

    @Test
    fun `given GB country code, when config provide, then GB config returned`() {
        assertThat(sut.provideCountryConfigFor("GB"))
            .isInstanceOf(CardReaderConfigForGB::class.java)
    }

    @Test
    fun `given RU country code, when config provide, then unsupported config returned`() {
        assertThat(sut.provideCountryConfigFor("RU"))
            .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
    }

    @Test
    fun `given AU country code, when config provide, then Australia returned`() {
        assertThat(sut.provideCountryConfigFor("AU"))
            .isInstanceOf(CardReaderConfigForAustralia::class.java)
    }

    @Test
    fun `given primary expansion country, when config provide, then per-country config returned`() {
        assertThat(sut.provideCountryConfigFor("IE")).isInstanceOf(CardReaderConfigForIE::class.java)
        assertThat(sut.provideCountryConfigFor("NL")).isInstanceOf(CardReaderConfigForNL::class.java)
        assertThat(sut.provideCountryConfigFor("SG")).isInstanceOf(CardReaderConfigForSG::class.java)
        assertThat(sut.provideCountryConfigFor("NZ")).isInstanceOf(CardReaderConfigForNZ::class.java)
    }

    @Test
    fun `given EU extended country, when config provide, then per-country config returned`() {
        assertThat(sut.provideCountryConfigFor("FI")).isInstanceOf(CardReaderConfigForFI::class.java)
        assertThat(sut.provideCountryConfigFor("LU")).isInstanceOf(CardReaderConfigForLU::class.java)
    }

    @Test
    fun `given fiscalization country codes, when config provide, then unsupported returned`() {
        listOf("AT", "BE", "FR", "IT", "DE", "PT", "ES").forEach { code ->
            assertThat(sut.provideCountryConfigFor(code))
                .`as`("Expected $code to be unsupported")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
    }
}
