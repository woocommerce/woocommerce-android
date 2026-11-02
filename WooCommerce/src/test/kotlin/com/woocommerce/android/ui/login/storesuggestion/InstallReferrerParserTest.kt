package com.woocommerce.android.ui.login.storesuggestion

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URLEncoder

class InstallReferrerParserTest {
    private val sut = InstallReferrerParser()

    @Test
    fun `given happy path, when parse, then returns decoded campaign and site url`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com"))

        // THEN
        assertThat(result).isEqualTo(
            ParsedInstallReferrer(campaign = CAMPAIGN, hasSiteUrl = true, siteUrl = "https://example.com")
        )
    }

    @Test
    fun `given site url without scheme, when parse, then site url gets https scheme`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "example.com"))

        // THEN
        assertThat(result.siteUrl).isEqualTo("https://example.com")
    }

    @Test
    fun `given site url with http scheme, when parse, then http site url is returned`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "http://example.com"))

        // THEN
        assertThat(result.siteUrl).isEqualTo("http://example.com")
    }

    @Test
    fun `given site url with path, when parse, then path is kept`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com/shop"))

        // THEN
        assertThat(result.siteUrl).isEqualTo("https://example.com/shop")
    }

    @Test
    fun `given site url with trailing slash, when parse, then trailing slash is trimmed`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com/shop/"))

        // THEN
        assertThat(result.siteUrl).isEqualTo("https://example.com/shop")
    }

    @Test
    fun `given site url with surrounding whitespace, when parse, then whitespace is trimmed`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "  https://example.com  "))

        // THEN
        assertThat(result.siteUrl).isEqualTo("https://example.com")
    }

    @Test
    fun `given empty site url, when parse, then has no site url`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = ""))

        // THEN
        assertThat(result.hasSiteUrl).isFalse()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given blank site url, when parse, then has no site url`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "   "))

        // THEN
        assertThat(result.hasSiteUrl).isFalse()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given missing site url, when parse, then has no site url`() {
        // WHEN
        val result = sut.parse("utm_source=google-play&utm_campaign=$CAMPAIGN")

        // THEN
        assertThat(result.hasSiteUrl).isFalse()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with userinfo, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://user:pass@example.com"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with username only, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://user@example.com"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with query, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com?redirect=evil.com"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with fragment, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com#section"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with javascript scheme, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "javascript:alert(1)"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url with ftp scheme, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "ftp://example.com"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given site url host without a dot, when parse, then site url is invalid`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://localhost"))

        // THEN
        assertThat(result.hasSiteUrl).isTrue()
        assertThat(result.siteUrl).isNull()
    }

    @Test
    fun `given malformed percent encoding in site url, when parse, then site url is ignored`() {
        // WHEN
        val result = sut.parse("utm_campaign=$CAMPAIGN&site_url=https%3A%2F%2Fexample.com%E")

        // THEN
        assertThat(result).isEqualTo(ParsedInstallReferrer(campaign = CAMPAIGN, hasSiteUrl = false, siteUrl = null))
    }

    @Test
    fun `given malformed percent encoding in a key, when parse, then other params are still parsed`() {
        // WHEN
        val result = sut.parse("%zz=value&" + referrer(siteUrl = "https://example.com"))

        // THEN
        assertThat(result.siteUrl).isEqualTo("https://example.com")
    }

    @Test
    fun `given blank campaign, when parse, then campaign is null`() {
        // WHEN
        val result = sut.parse(referrer(siteUrl = "https://example.com", campaign = "%20"))

        // THEN
        assertThat(result.campaign).isNull()
    }

    @Test
    fun `given missing campaign, when parse, then campaign is null`() {
        // WHEN
        val result = sut.parse("site_url=${encode("https://example.com")}")

        // THEN
        assertThat(result.campaign).isNull()
    }

    @Test
    fun `given empty referrer, when parse, then nothing is parsed`() {
        // WHEN
        val result = sut.parse("")

        // THEN
        assertThat(result).isEqualTo(ParsedInstallReferrer(campaign = null, hasSiteUrl = false, siteUrl = null))
    }

    private fun referrer(siteUrl: String, campaign: String = CAMPAIGN) =
        "utm_source=google-play&utm_campaign=$campaign&site_url=${encode(siteUrl)}"

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val CAMPAIGN = "store_login"
    }
}
