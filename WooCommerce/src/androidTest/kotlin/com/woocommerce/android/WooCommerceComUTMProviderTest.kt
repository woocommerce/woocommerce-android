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
    fun `givenUrlThenSetDefaultUtmMediumToWoo_android`() {
        val url = "https://www.woocommerce.com"
        val expectedUrl = "$url?utm_medium=woo_android"
        val defaultUTMMedium = "woo_android"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }

    @Test
    fun `givenUrlWhenQueryParamsAreProvidedThenConstructUrlWithQueryParams`() {
        val utmCampaign = "feature_announcement_card"
        val utmSource = "orders_list"
        val utmContent = "upsell_card_readers"
        val url = "https://www.woocommerce.com?utm_campaign=${utmCampaign}&utm_source=${utmSource}"
        val expectedUrl = "https://www.woocommerce.com?utm_campaign=${utmCampaign}&utm_source=${utmSource}" +
            "&utm_content=${utmContent}&utm_term=1234&utm_medium=woo_android"
        val defaultUTMMedium = "woo_android"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = utmSource,
            content = utmContent,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM.getQueryParameter("utm_source")).isEqualTo(utmSource)
        assertThat(urlWithUTM.getQueryParameter("utm_content")).isEqualTo(utmContent)
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }

    @Test
    fun `queryStringExcludesAllParamsThatAreEmptyOrNull`() {
        val utmCampaign = "feature_announcement_card"
        val defaultUTMMedium = "woo_android"
        val url = "https://www.woocommerce.com/us/hw?utm_campaign=payments_menu_item"
        val expectedUrl = "https://www.woocommerce.com/us/hw?utm_campaign=${utmCampaign}" +
            "&utm_term=1234&utm_medium=${defaultUTMMedium}"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = "",
            content = null,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertFalse(urlWithUTM.toString().contains("utm_source"))
        assertFalse(urlWithUTM.toString().contains("utm_content"))
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }

    @Test
    fun `givenUrlWithUtmParamsWhenQueryParamsAreProvidedThenConstructUrlByOverridingUrlParamsWithProvidedUtmParams`() {
        val utmCampaign = "feature_announcement_card"
        val utmSource = "orders_list"
        val utmContent = "upsell_card_readers"
        val defaultUTMMedium = "woo_android"
        val url = "https://www.woocommerce.com/us/hw?utm_campaign=payments_menu_item&utm_source=payments_menu"
        val expectedUrl = "https://www.woocommerce.com/us/hw?utm_campaign=${utmCampaign}&utm_source=${utmSource}" +
            "&utm_content=${utmContent}&utm_term=1234&utm_medium=${defaultUTMMedium}"

        val urlWithUTM = provideUTMProvider(
            campaign = utmCampaign,
            source = utmSource,
            content = utmContent,
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("utm_medium")).isEqualTo(defaultUTMMedium)
        assertThat(urlWithUTM.getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM.getQueryParameter("utm_source")).isEqualTo(utmSource)
        assertThat(urlWithUTM.getQueryParameter("utm_content")).isEqualTo(utmContent)
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }

    @Test
    fun `givenUrlWithOtherParamsThenConstructUrlByKeepingOtherUrlParams`() {
        val url = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item&test_utm_source=payments_menu"
        val expectedUrl = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item&test_utm_source=payments_menu" +
            "&utm_medium=woo_android"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertTrue(urlWithUTM.toString().contains("test_utm_campaign"))
        assertTrue(urlWithUTM.toString().contains("test_utm_source"))
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }

    @Test
    fun `givenUrlWithOtherParamsThenConstructUrlByKeepingOtherUrlParamsWithCorrectValues`() {
        val url = "https://www.woocommerce.com?test_utm_campaign=payments_menu_item&test_utm_source=payments_menu"

        val urlWithUTM = provideDefaultUTMProvider().getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("test_utm_campaign")).isEqualTo("payments_menu_item")
        assertThat(urlWithUTM.getQueryParameter("test_utm_source")).isEqualTo("payments_menu")
    }

    @Test
    fun `givenUrlWithUtmParamsWhenQueryParamsAreProvidedThenConstructUrlByOverridingOnlyTheMissingUtmParams`() {
        val utmCampaign = "feature_announcement_card"
        val url = "https://www.woocommerce.com?utm_campaign=${utmCampaign}"
        val expectedUrl = "$url&utm_term=1234&utm_medium=woo_android"

        val urlWithUTM = provideUTMProvider(
            campaign = "",
            source = "",
            content = "",
            siteId = 1234L
        ).getUrlWithUtmParams(url.toUri())

        assertThat(urlWithUTM.getQueryParameter("utm_campaign")).isEqualTo(utmCampaign)
        assertThat(urlWithUTM.toString()).isEqualTo(expectedUrl)
    }
}
