package com.woocommerce.android.ui.login.sitecredentials

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ApplicationPasswordSiteMatcherTest {
    @Test
    fun `given the same URL, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a different scheme, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "http://site.example")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a port, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://site.example:8443")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a path, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(
            SITE_URL,
            "https://site.example/wp/wp-admin/authorize-application.php?app_name=woo"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a www prefix, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://www.site.example")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address with a www prefix, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite("https://www.site.example", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with an uppercase host, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://WWW.Site.EXAMPLE")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address without a scheme, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite("site.example", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address with surrounding whitespace, when isSameSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite("  $SITE_URL  ", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate on another host, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://other.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a lookalike candidate host, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://site.example.evil.test")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate on a subdomain, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://shop.site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a doubled www prefix, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://www.www.site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a null candidate, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, null)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a blank candidate, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "   ")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a blank site address, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite("", SITE_URL)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a non-http scheme, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "ftp://site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a username, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://merchant@site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a username and password, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://merchant:secret@site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate whose user info hides another host, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://site.example@other.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given an unparseable candidate, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with an invalid host, when isSameSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSite(SITE_URL, "https://site example")

        // THEN
        assertThat(result).isFalse()
    }

    private companion object {
        const val SITE_URL = "https://site.example"
    }
}
