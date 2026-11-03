package com.woocommerce.android.ui.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SiteHostRulesTest {
    @Test
    fun `given plain hostnames, when normalize, then they are accepted in lowercase without a trailing dot`() {
        val cases = mapOf(
            "store.com" to "store.com",
            "Store.COM." to "store.com",
            "shop.store.co.uk" to "shop.store.co.uk",
            "my-store.com" to "my-store.com",
            "latest.com" to "latest.com",
            "test-shop.com" to "test-shop.com",
            "example-store.net" to "example-store.net",
            MAX_LENGTH_LABEL_HOST to MAX_LENGTH_LABEL_HOST,
            MAX_LENGTH_HOST to MAX_LENGTH_HOST,
        )

        cases.forEach { (input, expected) ->
            assertThat(SiteHostRules.normalize(input)).describedAs(input).isEqualTo(expected)
        }
    }

    @Test
    fun `given anything that is not a plain public ASCII hostname, when normalize, then it is rejected`() {
        val rejected = listOf(
            // URL parts
            "https://store.com", "http://store.com", "real.com@evil.com", "real.com:8443", "real.com/shop",
            "real.com?x=1", "real.com#a", "real.com\\evil.com", "real.com%2Fevil.com", "real.com evil.com", "",
            // Malformed labels and lengths
            "a..com", "-a.com", "a-.com", "a_b.com", "store.c0m", "store.123", TOO_LONG_LABEL_HOST, TOO_LONG_HOST,
            // IP addresses and local names
            "192.168.0.1", "127.1", "0x7f.0x1", "[::1]", "localhost", "intranet", "printer.local", "foo.internal",
            "x.localhost", "x.test", "x.invalid", "x.example", "router.home.arpa",
            // Non-ASCII and punycode
            "wоocommerce.com", "münchen.de", "xn--mnchen-3ya.de", "shop.xn--mnchen-3ya.com",
            "example。com", "store‮.com",
        )

        rejected.forEach { input ->
            assertThat(SiteHostRules.normalize(input)).describedAs(input).isNull()
        }
    }

    private companion object {
        val MAX_LENGTH_LABEL_HOST = "${"a".repeat(63)}.com"
        val TOO_LONG_LABEL_HOST = "${"a".repeat(64)}.com"
        val MAX_LENGTH_HOST = "${"a".repeat(63)}.${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(57)}.com"
        val TOO_LONG_HOST = "${"a".repeat(63)}.${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(58)}.com"
    }
}
