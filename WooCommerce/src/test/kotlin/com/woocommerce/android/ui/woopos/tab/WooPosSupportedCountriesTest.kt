package com.woocommerce.android.ui.woopos.tab

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosSupportedCountriesTest {

    @Test
    fun `when the supported countries are read, then they match the iOS POSCountryCurrencyValidator table`() {
        assertThat(WooPosSupportedCountries.countryCodes)
            .containsExactlyInAnyOrder("US", "PR", "GB", "CA", "FI", "IE", "LU", "NL", "SG", "NZ", "AU")
    }

    @Test
    fun `when the supported currencies are read, then they match the iOS POSCountryCurrencyValidator table`() {
        val expected = mapOf(
            "US" to setOf("USD"),
            "PR" to setOf("USD"),
            "GB" to setOf("GBP"),
            "CA" to setOf("CAD"),
            "FI" to setOf("EUR"),
            "IE" to setOf("EUR"),
            "LU" to setOf("EUR"),
            "NL" to setOf("EUR"),
            "SG" to setOf("SGD"),
            "NZ" to setOf("NZD"),
            "AU" to setOf("AUD"),
        )

        expected.forEach { (country, currencies) ->
            assertThat(WooPosSupportedCountries.currenciesFor(country))
                .describedAs(country)
                .isEqualTo(currencies)
        }
    }

    @Test
    fun `given a lowercase country code, when queried, then it matches case-insensitively`() {
        assertThat(WooPosSupportedCountries.isSupported("gb")).isTrue
        assertThat(WooPosSupportedCountries.currenciesFor("gb")).containsExactly("GBP")
    }

    @Test
    fun `given a country outside the table, when queried, then it is unsupported and has no currencies`() {
        assertThat(WooPosSupportedCountries.isSupported("DE")).isFalse
        assertThat(WooPosSupportedCountries.currenciesFor("DE")).isEmpty()
    }
}
