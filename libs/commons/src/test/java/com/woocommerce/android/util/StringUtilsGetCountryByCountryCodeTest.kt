package com.woocommerce.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringUtilsGetCountryByCountryCodeTest {
    @Test
    fun `given a country and state code, when resolving the country, then the country name is returned`() {
        assertThat(StringUtils.getCountryByCountryCode("US:NY")).isEqualTo("United States")
    }

    @Test
    fun `given a country code only, when resolving the country, then the country name is returned`() {
        assertThat(StringUtils.getCountryByCountryCode("US")).isEqualTo("United States")
    }

    @Test
    fun `given a lowercase country code, when resolving the country, then the country name is returned`() {
        assertThat(StringUtils.getCountryByCountryCode("us:ny")).isEqualTo("United States")
    }

    @Test
    fun `given a country whose name differs from the CLDR one, when resolving it, then WooCommerce name is used`() {
        assertThat(StringUtils.getCountryByCountryCode("CZ")).isEqualTo("Czech Republic")
        assertThat(StringUtils.getCountryByCountryCode("HK")).isEqualTo("Hong Kong")
        assertThat(StringUtils.getCountryByCountryCode("TR")).isEqualTo("Turkey")
    }

    @Test
    fun `given null, when resolving the country, then null is returned`() {
        assertThat(StringUtils.getCountryByCountryCode(null)).isNull()
    }

    @Test
    fun `given an empty string, when resolving the country, then null is returned`() {
        assertThat(StringUtils.getCountryByCountryCode("")).isNull()
    }

    @Test
    fun `given a state separator only, when resolving the country, then null is returned`() {
        assertThat(StringUtils.getCountryByCountryCode(":NY")).isNull()
    }

    @Test
    fun `given an unknown country code, when resolving the country, then null is returned`() {
        assertThat(StringUtils.getCountryByCountryCode("ZZ")).isNull()
    }

    @Test
    fun `given every ISO country code we map, when resolving it, then a non blank name is returned`() {
        COUNTRY_NAMES_BY_CODE.keys.forEach { code ->
            assertThat(StringUtils.getCountryByCountryCode(code))
                .describedAs("country name for %s", code)
                .isNotBlank()
        }
    }
}
