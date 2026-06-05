package com.woocommerce.android.cardreader.internal.config

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class CardReaderConfigIsPosCardPaymentEnabledTest {

    @RunWith(Parameterized::class)
    class PosEnabledCountriesTest(private val countryCode: String) {

        @Test
        fun `given a POS card-payment country, when isPosCardPaymentEnabled is read, then it returns true`() {
            val config = CardReaderConfigFactory().getCardReaderConfigFor(countryCode)

            assertThat(config.isPosCardPaymentEnabled).isTrue
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "country: {0}")
            fun data(): List<String> = listOf(
                "US", "PR", "GB",
                "FR", "DE", "IE", "NL", "AT", "BE", "FI", "IT", "LU", "PT", "ES",
                "SG", "NZ",
                "AU",
            )
        }
    }

    @RunWith(Parameterized::class)
    class PosDisabledUnsupportedCountriesTest(private val countryCode: String) {

        @Test
        fun `given an unsupported country, when isPosCardPaymentEnabled is read, then it returns false`() {
            val config = CardReaderConfigFactory().getCardReaderConfigFor(countryCode)

            assertThat(config).isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
            assertThat(config.isPosCardPaymentEnabled).isFalse
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "country: {0}")
            fun data(): List<String> = listOf("JP", "MX", "IN", "BR", "invalid")
        }
    }

    class CanadaOverrideTest {

        @Test
        fun `given country CA, when isPosCardPaymentEnabled is read, then it returns false even though IPP is supported`() {
            val config = CardReaderConfigFactory().getCardReaderConfigFor("CA")

            assertThat(config).isInstanceOf(CardReaderConfigForCanada::class.java)
            assertThat(config).isInstanceOf(CardReaderConfigForSupportedCountry::class.java)
            assertThat(config.isPosCardPaymentEnabled).isFalse
        }
    }
}
