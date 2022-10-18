package com.woocommerce.android

import androidx.core.net.toUri
import com.woocommerce.android.util.WooCommerceComUTMProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WooCommerceComUTMProviderTest {

    private fun provideDefaultUTMProvider() = WooCommerceComUTMProvider(
        campaign = "",
        source = "",
        content = "",
        siteId = null
    )

    private fun provideUTMProvider(
        campaign: String,
        source: String,
        content: String?,
        siteId: Long?
    ) = WooCommerceComUTMProvider(
        campaign = campaign,
        source = source,
        content = content,
        siteId = siteId
    )

    @Test
    fun `testUtmMediumAlwaysSetToWoo_android`() {
        val url = "https://www.woocommerce.com"
        val expectedUrl = "$url?utm_medium=woo_android"
        val defaultUTMMedium = "woo_android"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testUtmQueryParamsAreAddedToTheUrlProperly`() {
        val utmCampaign = "feature_announcement_card"
        val utmSource = "orders_list"
        val utmContent = "upsell_card_readers"
        val url = "https://www.woocommerce.com?utm_campaign=$utmCampaign&utm_source=$utmSource"
        val expectedUrl = "https://www.woocommerce.com?utm_campaign=$utmCampaign&utm_source=$utmSource" +
            "&utm_content=$utmContent&utm_term=1234&utm_medium=woo_android"
        val defaultUTMMedium = "woo_android"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = utmSource,
            content = utmContent,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_source")).isEqualTo(utmSource)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_content")).isEqualTo(utmContent)
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testUtmQueriesAreExcludedInTheUrlIfTheyAreNullOrEmpty`() {
        val utmCampaign = "feature_announcement_card"
        val defaultUTMMedium = "woo_android"
        val url = "https://www.woocommerce.com/us/hw"
        val expectedUrl = "https://www.woocommerce.com/us/hw?utm_campaign=$utmCampaign" +
            "&utm_term=1234&utm_medium=$defaultUTMMedium"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = "",
            content = null,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertFalse(urlWithUTM.contains("utm_source"))
        assertFalse(urlWithUTM.contains("utm_content"))
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testSuppliedUtmQueriesOverridesExistingUrlUtmQueries`() {
        val existingUtmCampaign = "payments_menu_item"
        val existingUtmSource = "payments_menu"
        val utmCampaign = "feature_announcement_card"
        val utmSource = "orders_list"
        val utmContent = "upsell_card_readers"
        val defaultUTMMedium = "woo_android"
        val url = "https://www.woocommerce.com/us/hw?utm_campaign=${existingUtmCampaign}" +
            "&utm_source=${existingUtmSource}"
        val expectedUrl = "https://www.woocommerce.com/us/hw?utm_campaign=$utmCampaign&utm_source=$utmSource" +
            "&utm_content=$utmContent&utm_term=1234&utm_medium=$defaultUTMMedium"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = utmSource,
            content = utmContent,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_source")).isEqualTo(utmSource)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_content")).isEqualTo(utmContent)
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testExistingUrlUtmQueriesArePreservedIfSuppliedUtmQueriesAreInvalid`() {
        val existingUtmCampaign = "payments_menu_item"
        val existingUtmSource = "payments_menu"
        val newUtmCampaign = ""
        val newUtmSource = ""
        val newUtmContent = null
        val defaultUTMMedium = "woo_android"
        val url = "https://www.woocommerce.com/us/hw?utm_campaign=${existingUtmCampaign}" +
            "&utm_source=${existingUtmSource}"
        val expectedUrl = "https://www.woocommerce.com/us/hw?utm_campaign=$existingUtmCampaign" +
            "&utm_source=$existingUtmSource&utm_term=1234&utm_medium=$defaultUTMMedium"

        val urlWithUTM = provideUTMProvider(
            campaign = newUtmCampaign,
            source = newUtmSource,
            content = newUtmContent,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_campaign")).isEqualTo(existingUtmCampaign)
        assertThat(urlWithUTM.toUri().getQueryParameter("utm_source")).isEqualTo(existingUtmSource)
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testUrlQueriesArePreservedIfTheyAreValid`() {
        val url = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item&test_utm_source=payments_menu"
        val expectedUrl = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item" +
            "&test_utm_source=payments_menu&utm_medium=woo_android"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertTrue(urlWithUTM.contains("test_utm_campaign"))
        assertTrue(urlWithUTM.contains("test_utm_source"))
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }

    @Test
    fun `testUrlQueriesArePreservedWithCorrectValuesIfTheyAreValid`() {
        val url = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item&test_utm_source=payments_menu"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("test_utm_campaign")).isEqualTo("payments_menu_item")
        assertThat(urlWithUTM.toUri().getQueryParameter("test_utm_source")).isEqualTo("payments_menu")
    }

    @Test
    fun `testOverrideOnlyTheMissingOrInvalidUtmQueries`() {
        val utmCampaign = "feature_announcement_card"
        val url = "https://www.woocommerce.com?utm_campaign=$utmCampaign"
        val expectedUrl = "$url&utm_term=1234&utm_medium=woo_android"

        val urlWithUTM = provideUTMProvider(
            campaign = "",
            source = "",
            content = "",
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.toUri().getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM).isEqualTo(expectedUrl)
    }
}
