package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

/**
 * Fetches a server-calculated refund preview for the selected items, transparently falling back to
 * the v3 + local-calculation flow when the store does not expose the v4 endpoints.
 *
 * Detection strategy (WOOMOB-2693):
 * 1. The v4 refund endpoints ship in WooCommerce [MIN_WC_VERSION_FOR_V4]. If the cached WooCommerce
 *    version is older, v4 cannot be available — skip the probe entirely (no network call) and fall
 *    back. The version is read from the local plugin cache, so this is free.
 * 2. On [MIN_WC_VERSION_FOR_V4]+ the endpoints may still be hidden behind a feature flag, so attempt
 *    the v4 preview call. A `404 rest_no_route` ([WooErrorType.API_NOT_FOUND]) means the route is not
 *    registered → mark v4 unavailable for the site and fall back. A successful call marks v4
 *    available so subsequent refunds skip the probe.
 *
 * The detection result is cached per site (see [WooPosV4RefundAvailabilityCache]).
 */
class WooPosRefundPreview @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val availabilityCache: WooPosV4RefundAvailabilityCache,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    suspend operator fun invoke(
        orderId: Long,
        lineItems: List<RefundV4LineItem>,
    ): Result {
        val site = selectedSite.get()

        if (availabilityCache.isV4Available(site.siteId) == false) {
            return Result.FallbackToLocal
        }

        if (isWooVersionBelowV4Support()) {
            WooLog.i(WooLog.T.POS, "WooPosRefund: WooCommerce older than $MIN_WC_VERSION_FOR_V4; using v3")
            availabilityCache.markV4Unavailable(site.siteId)
            return Result.FallbackToLocal
        }

        if (lineItems.isEmpty()) {
            // An empty request is rejected by the server with rest_invalid_param (400), not the
            // 404 we use for detection — so never probe with nothing selected.
            return Result.FallbackToLocal
        }

        val response = refundStore.previewRefund(site, orderId, lineItems)
        return when {
            response.isError -> {
                if (response.error.type == WooErrorType.API_NOT_FOUND) {
                    WooLog.i(WooLog.T.POS, "WooPosRefund: v4 preview not available; falling back to v3")
                    availabilityCache.markV4Unavailable(site.siteId)
                    Result.FallbackToLocal
                } else {
                    WooLog.e(
                        WooLog.T.POS,
                        "WooPosRefund: v4 preview failed orderId=$orderId, type=${response.error.type}, " +
                            "message=${response.error.message}"
                    )
                    Result.Error
                }
            }

            response.model != null -> {
                availabilityCache.markV4Available(site.siteId)
                Result.ServerCalculated(response.model!!)
            }

            else -> Result.Error
        }
    }

    /**
     * @return `true` only when the cached WooCommerce version is known and older than the first
     * version that ships the v4 refund endpoints. An unknown (null) version is not treated as below
     * support, so detection still falls through to the probe.
     */
    private fun isWooVersionBelowV4Support(): Boolean {
        val version = getWooCoreVersion() ?: return false
        return version.semverCompareTo(MIN_WC_VERSION_FOR_V4) < 0
    }

    sealed interface Result {
        data class ServerCalculated(val preview: WCRefundPreview) : Result
        data object FallbackToLocal : Result
        data object Error : Result
    }

    private companion object {
        private const val MIN_WC_VERSION_FOR_V4 = "10.9.0"
    }
}
