package com.woocommerce.android.utils

import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import org.wordpress.android.util.LanguageUtils
import kotlin.math.absoluteValue

object WCSiteUtils {
    /**
     * Generates a single [WCSettingsModel] object for a mock Site
     */
    fun generateSiteSettings(
        localSiteId: Int = 1,
        currencyCode: String = "USD",
        currencyPosition: CurrencyPosition = LEFT
    ): WCSettingsModel {
        return WCSettingsModel(
                localSiteId = localSiteId,
                currencyCode = currencyCode,
                currencyPosition = currencyPosition,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2)
    }

    /**
     * Mock of the method in FluxC
     * Not sure if this is the best way to mock SiteSettings in FluxC.
     *
     * So the issue is, formatting currency for display, depends on the store's settings.
     * If the store settings is null, then the currency by default is displayed with only 1 decimal place,
     * or in some cases no decimal places. But default behaviour is to display currency code to the left
     * with 2 decimal places.
     * In order to correctly validate the currency formatting, we need to mock the `getSiteSettings` method
     * inside `WooCommerceStore` class. This method is called directly from FluxC when trying to format
     * currency.
     *
     * The `WooCommerceStore` is final so we cannot mock/spy the class.
     * So a simple
     * doReturn(WcOrderTestUtils.generateSiteSettings()).whenever(mockWcStore).getSiteSettings(any())
     * will result in cannot mock/spy the class error.
     *
     * This approach was to mock the CurrencyFormatter class. But the only logic in this class is to call
     * the appropriate method in FluxC. So rather than calling this method in CurrencyFormatter class,
     * we can mock this method to return the appropriate value. But the logic to format is still retained in
     * fluxC so we would need to replicate this logic in this method and check if the value matches.
     * I admit I am not sure if this is the best approach since we would only be replicating the same logic
     * from FluxC into Woo and that too only for UI testing purposes.
     * If in future the logic is changed in FluxC, the UI tests would fail and we would need to replicate this
     * inside Woo UI test as well.
     *
     * Another approach would be to send the SiteSettingsModel to the format currency method in `WooCommerceStore`.
     * This way we can mock the method to fetch the SiteSettings in Woo itself and then pass the value to
     * FluxC. This would mean making modifications to FluxC and I don't see this as really useful for anything
     * other than fixing this issue since we would be fetching the SiteSettingsModel from FluxC and then
     * passing the same to another method in FluxC instead of keeping the entire logic inside a single method.
     *
     * I ran out of ideas at this point other than to just insert a mock WcSiteSettingsModel directly into the local db
     * when UI tests first start, which seems like a bad idea!
     */
    fun formatCurrencyForDisplay(
        rawValue: String,
        siteSettings: WCSettingsModel?,
        currencyCode: String? = null,
        applyDecimalFormatting: Boolean
    ): String {
        // Resolve the currency code to a localized symbol
        val resolvedCurrencyCode = currencyCode ?: siteSettings?.currencyCode
        val currencySymbol = resolvedCurrencyCode?.let {
            WCCurrencyUtils.getLocalizedCurrencySymbolForCode(it, LanguageUtils.getCurrentDeviceLanguage())
        } ?: ""

        // Format the amount for display according to the site's currency settings
        // Use absolute values - if the value is negative, it will be handled in the next step, with the currency symbol
        val decimalFormattedValue = siteSettings?.takeIf { applyDecimalFormatting }?.let {
            WCCurrencyUtils.formatCurrencyForDisplay(rawValue.toDoubleOrNull()?.absoluteValue ?: 0.0, it)
        } ?: rawValue.removePrefix("-")

        // Append or prepend the currency symbol according to the site's settings
        with(StringBuilder()) {
            if (rawValue.startsWith("-")) { append("-") }
            append(when (siteSettings?.currencyPosition) {
                null, LEFT -> "$currencySymbol$decimalFormattedValue"
                LEFT_SPACE -> "$currencySymbol $decimalFormattedValue"
                RIGHT -> "$decimalFormattedValue$currencySymbol"
                RIGHT_SPACE -> "$decimalFormattedValue $currencySymbol"
            })
            return toString()
        }
    }
}
