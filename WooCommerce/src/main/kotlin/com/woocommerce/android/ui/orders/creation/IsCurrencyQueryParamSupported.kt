package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

/**
 * True when it's safe to send the `currency` query parameter to the store.
 *
 * WooPayments Multi-Currency persists the currency it reads from that parameter (into the user meta of the
 * authenticated user, or into the WooCommerce session), and only ignores the persisted value on requests
 * WooCommerce recognises as REST. Before 11.1.0, `is_rest_api_request()` missed the `?rest_route=` form, so a
 * single request carrying the parameter left the store converting the prices of every later request that didn't
 * carry one.
 *
 * This deliberately doesn't look at the networking transport: an Application Passwords request can fall back to
 * the Jetpack tunnel mid-flight, and Application Passwords themselves use `?rest_route=` on sites without pretty
 * permalinks.
 *
 * An unknown version resolves to `false`. Both outcomes are bad — without the parameter the line item is priced
 * in the store's currency, so the order total is wrong — but that is how stores below the fix version behave
 * regardless, it is confined to the order being edited, and the merchant can see and correct it. Persisting a
 * currency the store then applies to every later request is neither visible nor correctable from the app.
 */
class IsCurrencyQueryParamSupported @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val fetchWooVersion: FetchActiveWCPluginVersion,
) {
    suspend operator fun invoke(): Boolean {
        val version = getWooCoreVersion() ?: fetchWooVersion() ?: return false
        return version.semverCompareTo(WC_CURRENCY_QUERY_PARAM_FIX_VERSION) >= 0
    }

    companion object {
        const val WC_CURRENCY_QUERY_PARAM_FIX_VERSION = "11.1.0"
    }
}
