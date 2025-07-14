package org.wordpress.android.fluxc.wc.settings

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.WCSettingsMapper
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse.Authentication
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingOptionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient.SSRResponse
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel

object WCSettingsTestUtils {

    internal fun generateSettingsModel(siteId: LocalId) = WCSettingsModel(
        localSiteId = siteId,
        currencyCode = "USD",
        currencyPosition = CurrencyPosition.LEFT,
        currencyThousandSeparator = ",",
        currencyDecimalSeparator = ".",
        currencyDecimalNumber = 2,
        countryCode = "US",
        stateCode = "CA",
        address = "123 Main St",
        address2 = "Apt 4",
        city = "San Francisco",
        postalCode = "94105",
        couponsEnabled = false
    )

    fun generateSettings(siteId: LocalId) = generateSettingsModel(siteId).let {
        WCSettingsMapper.mapToDomain(it)
    }

    fun getSiteSettingsResponse() =
        "wc/site-settings-general-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()

    fun getSiteProductSettingsResponse() =
        "wc/product-settings-response.json"
            .jsonFileAs(Array<SiteSettingsResponse>::class.java)
            ?.toList()

    fun getTaxBasedOnSettingsResponse() =
        "wc/tax-based-on-settings-response.json"
            .jsonFileAs(SiteSettingOptionResponse::class.java)

    fun getSiteSettingOptionResponse() =
        "wc/site-setting-option-response.json"
            .jsonFileAs(SiteSettingOptionResponse::class.java)

    fun getSSRResponse() =
        "wc/system-status.json"
            .jsonFileAs(SSRResponse::class.java)

    fun getSupportedApiVersionResponse() =
        RootWPAPIRestResponse(
            authentication = Authentication(),
            namespaces = arrayListOf("wc/v3")
        )

    fun getUnsupportedApiVersionResponse() =
        RootWPAPIRestResponse(
            authentication = Authentication(),
            namespaces = arrayListOf("wc/v1")
        )
}
