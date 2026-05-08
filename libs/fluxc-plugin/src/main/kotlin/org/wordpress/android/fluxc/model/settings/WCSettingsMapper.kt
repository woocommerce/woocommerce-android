package org.wordpress.android.fluxc.model.settings

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.taxes.TaxBasedOnSettingEntity
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingOptionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingsResponse
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel
import javax.inject.Inject

class WCSettingsMapper
@Inject constructor() {

    @Suppress("CyclomaticComplexMethod")
    internal fun mapSiteSettings(response: List<SiteSettingsResponse>, site: SiteModel): WCSettingsModel {
        val currencyCode = getValueForSettingsField(response, "woocommerce_currency")
        val currencyPosition = getValueForSettingsField(response, "woocommerce_currency_pos")
        val currencyThousandSep = getValueForSettingsField(response, "woocommerce_price_thousand_sep")
        val currencyDecimalSep = getValueForSettingsField(response, "woocommerce_price_decimal_sep")
        val currencyNumDecimals = getValueForSettingsField(response, "woocommerce_price_num_decimals")
        val address = getValueForSettingsField(response, "woocommerce_store_address")
        val address2 = getValueForSettingsField(response, "woocommerce_store_address_2")
        val city = getValueForSettingsField(response, "woocommerce_store_city")
        val postalCode = getValueForSettingsField(response, "woocommerce_store_postcode")
        val countryAndState = getValueForSettingsField(response, "woocommerce_default_country")
            ?.split(":")
        val country = countryAndState?.firstOrNull()
        val state = countryAndState?.getOrNull(1)
        val couponsEnabled = getValueForSettingsField(response, "woocommerce_enable_coupons")

        return WCSettingsModel(
            localSiteId = site.localId(),
            currencyCode = currencyCode ?: "",
            currencyPosition = currencyPosition.toCurrencyPositionOrDefault(),
            currencyThousandSeparator = currencyThousandSep ?: "",
            currencyDecimalSeparator = currencyDecimalSep ?: "",
            currencyDecimalNumber = currencyNumDecimals?.toIntOrNull() ?: 2,
            countryCode = country ?: "",
            stateCode = state ?: "",
            address = address ?: "",
            address2 = address2 ?: "",
            city = city ?: "",
            postalCode = postalCode ?: "",
            couponsEnabled = couponsEnabled?.let { it == "yes" } ?: false
        )
    }

    private fun String?.toCurrencyPositionOrDefault(): CurrencyPosition = this?.let {
        CurrencyPosition.entries.find { entry -> entry.name.equals(it, ignoreCase = true) }
    } ?: CurrencyPosition.LEFT

    fun mapProductSettings(response: List<SiteSettingsResponse>, site: SiteModel): WCProductSettingsModel {
        val weightUnit = getValueForSettingsField(response, "woocommerce_weight_unit")
        val dimensionUnit = getValueForSettingsField(response, "woocommerce_dimension_unit")
        val defaultLowStockThreshold = getValueForSettingsField(response, "woocommerce_notify_low_stock_amount")
            ?.toIntOrNull()

        return WCProductSettingsModel(
            localSiteId = site.localId(),
            dimensionUnit = dimensionUnit ?: "",
            weightUnit = weightUnit ?: "",
            defaultLowStockThreshold = defaultLowStockThreshold,
        )
    }

    fun mapTaxBasedOnSettings(
        response: SiteSettingOptionResponse,
        localSiteId: LocalId
    ): TaxBasedOnSettingEntity {
        return TaxBasedOnSettingEntity(
            localSiteId = localSiteId,
            selectedOption = response.value ?: "",
        )
    }

    private fun getValueForSettingsField(settingsResponse: List<SiteSettingsResponse>, field: String): String? {
        return settingsResponse.find { it.id != null && it.id == field }?.value?.asString
    }

    companion object {
        internal fun mapToDomain(entity: WCSettingsModel): Settings {
            return Settings(
                currencyCode = entity.currencyCode,
                currencyPosition = entity.currencyPosition,
                currencyThousandSeparator = entity.currencyThousandSeparator,
                currencyDecimalSeparator = entity.currencyDecimalSeparator,
                currencyDecimalNumber = entity.currencyDecimalNumber,
                countryCode = entity.countryCode,
                stateCode = entity.stateCode,
                address = entity.address,
                address2 = entity.address2,
                city = entity.city,
                postalCode = entity.postalCode,
                couponsEnabled = entity.couponsEnabled
            )
        }
    }
}
