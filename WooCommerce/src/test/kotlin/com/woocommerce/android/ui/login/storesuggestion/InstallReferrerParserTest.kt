package com.woocommerce.android.ui.login.storesuggestion

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InstallReferrerParserTest {
    private val sut = InstallReferrerParser()

    @Test
    fun `given valid referrer, when parse, then returns campaign and normalized host`() {
        // WHEN
        val result = sut.parse("utm_source=google-play&utm_campaign=$CAMPAIGN&site_host=Shop.Store.COM")

        // THEN
        assertThat(result).isEqualTo(ParsedInstallReferrer(campaign = CAMPAIGN, siteHost = SiteHost.Valid(HOST)))
    }

    @Test
    fun `given referrer without site host, when parse, then site host is missing`() {
        val referrers = listOf(
            "utm_campaign=$CAMPAIGN",
            "utm_campaign=$CAMPAIGN&site_url=https%3A%2F%2F$HOST",
            "",
        )

        referrers.forEach { referrer ->
            assertThat(sut.parse(referrer).siteHost).describedAs(referrer).isEqualTo(SiteHost.Missing)
        }
    }

    @Test
    fun `given unusable site host value, when parse, then site host is invalid`() {
        val referrers = listOf(
            "site_host=$HOST&site_host=$HOST",
            "site_host=$HOST&site_host=other.com",
            "site_host=store.com%E",
            "site_host=a+b.com",
            "site_host=a.com%26site_host%3Devil.com",
            "site_host=https%3A%2F%2F$HOST",
            "site_host=",
        )

        referrers.forEach { referrer ->
            assertThat(sut.parse(referrer).siteHost).describedAs(referrer).isEqualTo(SiteHost.Invalid)
        }
    }

    @Test
    fun `given malformed key elsewhere, when parse, then other params are still read`() {
        // WHEN
        val result = sut.parse("%zz=value&utm_campaign=$CAMPAIGN&site_host=$HOST")

        // THEN
        assertThat(result).isEqualTo(ParsedInstallReferrer(campaign = CAMPAIGN, siteHost = SiteHost.Valid(HOST)))
    }

    @Test
    fun `given blank or missing campaign, when parse, then campaign is null`() {
        listOf("utm_campaign=%20&site_host=$HOST", "site_host=$HOST").forEach { referrer ->
            assertThat(sut.parse(referrer).campaign).describedAs(referrer).isNull()
        }
    }

    private companion object {
        const val CAMPAIGN = "deeplinks_promote_app"
        const val HOST = "shop.store.com"
    }
}
