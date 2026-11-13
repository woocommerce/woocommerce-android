package com.woocommerce.android.ui.login.storesuggestion

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SplitHostForEmphasisTest {
    @Test
    fun `given subdomain of the registrable domain, when split, then only the domain is emphasized`() {
        // WHEN
        val result = splitHostForEmphasis(host = "shop.store.com", registrableDomain = "store.com")

        // THEN
        assertThat(result).isEqualTo("shop." to "store.com")
    }

    @Test
    fun `given host equal to the registrable domain, when split, then the whole host is emphasized`() {
        // WHEN
        val result = splitHostForEmphasis(host = "store.com", registrableDomain = "store.com")

        // THEN
        assertThat(result).isEqualTo("" to "store.com")
    }

    @Test
    fun `given no registrable domain, when split, then the whole host is emphasized`() {
        // WHEN
        val result = splitHostForEmphasis(host = "shop.store.com", registrableDomain = null)

        // THEN
        assertThat(result).isEqualTo("" to "shop.store.com")
    }

    @Test
    fun `given registrable domain that only matches as a text suffix, when split, then the whole host is emphasized`() {
        // WHEN
        val result = splitHostForEmphasis(host = "evilstore.com", registrableDomain = "store.com")

        // THEN
        assertThat(result).isEqualTo("" to "evilstore.com")
    }
}
